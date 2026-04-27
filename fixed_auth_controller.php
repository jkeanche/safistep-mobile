<?php
namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\OtpCode;
use App\Models\User;
use App\Services\MobitechService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Crypt;
use Illuminate\Validation\ValidationException;
use Laravel\Sanctum\PersonalAccessToken;

class AuthController extends Controller
{
    public function __construct(private MobitechService $sms) {}

    /**
     * Normalize phone number to Kenya format
     */
    private function normalizePhone(string $phone): string
    {
        $cleaned = preg_replace('/\s+/', '', trim($phone));
        $cleaned = ltrim($cleaned, '+');
        
        if (str_starts_with($cleaned, '254')) {
            return $cleaned;
        }
        
        if (str_starts_with($cleaned, '0')) {
            return '254' . substr($cleaned, 1);
        }
        
        return '254' . $cleaned;
    }

    /**
     * POST /api/v1/auth/request-otp
     * Sends OTP to Safaricom number. Works for both registration & password reset.
     */
    public function requestOtp(Request $request)
    {
        Log::info('Incoming OTP request:', $request->all());
        
        try {
            $request->validate([
                'phone'   => 'required|string|regex:/^(\+?254|0)[17]\d{8}$/',
                'purpose' => 'in:registration,password_reset',
            ]);

            $phone   = $this->normalizePhone($request->phone);
            $purpose = $request->purpose ?? 'registration';

            Log::info('Normalized phone: ' . $phone . ', purpose: ' . $purpose);

            // Invalidate previous unused OTPs
            OtpCode::where('phone', $phone)->where('used', false)->update(['used' => true]);

            $otp = $this->sms->sendOtp($phone);

            if (!$otp) {
                Log::error('SMS service failed to send OTP', ['phone' => $phone]);
                Log::error('SMS service response:', ['otp' => $otp]);
                return response()->json(['message' => 'Failed to send OTP. Try again.'], 503);
            }

            Log::info('OTP generated successfully', ['phone' => $phone, 'otp' => $otp]);

            OtpCode::create([
                'phone'      => $phone,
                'code'       => $otp,
                'purpose'    => $purpose,
                'expires_at' => now()->addMinutes(5),
            ]);

            return response()->json(['message' => 'OTP sent to your phone.']);
            
        } catch (ValidationException $e) {
            Log::error('Validation error in requestOtp:', $e->errors());
            return response()->json(['message' => 'Validation failed', 'errors' => $e->errors()], 422);
        } catch (\Exception $e) {
            Log::error('Unexpected error in requestOtp:', [
                'message' => $e->getMessage(),
                'file' => $e->getFile(),
                'line' => $e->getLine(),
                'trace' => $e->getTraceAsString()
            ]);
            return response()->json(['message' => 'Server error. Please try again.'], 500);
        }
    }

    /**
     * POST /api/v1/auth/verify-otp
     * Verifies OTP and returns a temporary token for password setting.
     */
    public function verifyOtp(Request $request)
    {
        try {
            $request->validate([
                'phone' => 'required|string',
                'code'  => 'required|string|size:6',
            ]);

            $phone = $this->normalizePhone($request->phone);
            $otp   = OtpCode::where('phone', $phone)
                             ->where('code', $request->code)
                             ->where('used', false)
                             ->where('expires_at', '>', now())
                             ->latest()
                             ->first();

            if (!$otp) {
                Log::warning('Invalid OTP attempt', ['phone' => $phone, 'code' => $request->code]);
                return response()->json(['message' => 'Invalid or expired OTP.'], 422);
            }

            $otp->update(['used' => true]);

            // Mark user as verified if they exist
            $user = User::where('phone', $phone)->first();
            if ($user) {
                $user->update(['is_verified' => true]);
            }

            // Return a signed short-lived token to proceed to set-password
            $tempToken = Crypt::encrypt($phone . '|' . now()->addMinutes(10)->timestamp);

            return response()->json([
                'message'    => 'OTP verified.',
                'temp_token' => $tempToken,
                'phone'      => $phone,
            ]);
            
        } catch (ValidationException $e) {
            Log::error('Validation error in verifyOtp:', $e->errors());
            return response()->json(['message' => 'Validation failed', 'errors' => $e->errors()], 422);
        } catch (\Exception $e) {
            Log::error('Unexpected error in verifyOtp:', [
                'message' => $e->getMessage(),
                'file' => $e->getFile(),
                'line' => $e->getLine(),
                'trace' => $e->getTraceAsString()
            ]);
            return response()->json(['message' => 'Server error. Please try again.'], 500);
        }
    }

    /**
     * POST /api/v1/auth/set-password
     * Creates/updates user with password. Returns API token.
     */
    public function setPassword(Request $request)
    {
        try {
            $request->validate([
                'temp_token' => 'required|string',
                'password'   => 'required|string|min:6|confirmed',
                'name'       => 'nullable|string|max:100',
            ]);

            try {
                [$phone, $expiresAt] = explode('|', Crypt::decrypt($request->temp_token));
                if (now()->timestamp > (int)$expiresAt) {
                    return response()->json(['message' => 'Session expired. Request a new OTP.'], 422);
                }
            } catch (\Throwable $e) {
                Log::error('Token decryption failed:', ['error' => $e->getMessage()]);
                return response()->json(['message' => 'Invalid session token.'], 422);
            }

            $user = User::updateOrCreate(
                ['phone' => $phone],
                [
                    'name'        => $request->name,
                    'password'    => Hash::make($request->password),
                    'is_verified' => true,
                ]
            );

            // Create Sanctum token
            $token = $user->createToken('mobile-app');
            $plainTextToken = $token->plainTextToken;

            Log::info('User registered/updated successfully', ['phone' => $phone]);

            return response()->json([
                'message' => 'Account ready.',
                'token'   => $plainTextToken,
                'user'    => $this->userResource($user),
            ], 201);
            
        } catch (ValidationException $e) {
            Log::error('Validation error in setPassword:', $e->errors());
            return response()->json(['message' => 'Validation failed', 'errors' => $e->errors()], 422);
        } catch (\Exception $e) {
            Log::error('Unexpected error in setPassword:', [
                'message' => $e->getMessage(),
                'file' => $e->getFile(),
                'line' => $e->getLine(),
                'trace' => $e->getTraceAsString()
            ]);
            return response()->json(['message' => 'Server error. Please try again.'], 500);
        }
    }

    /**
     * POST /api/v1/auth/login
     */
    public function login(Request $request)
    {
        try {
            $request->validate([
                'phone'    => 'required|string',
                'password' => 'required|string',
            ]);

            $phone = $this->normalizePhone($request->phone);
            $user = User::where('phone', $phone)->first();

            if (!$user || !Hash::check($request->password, $user->password)) {
                Log::warning('Failed login attempt', ['phone' => $phone]);
                throw ValidationException::withMessages(['phone' => 'Invalid credentials.']);
            }

            if (!$user->is_verified) {
                return response()->json(['message' => 'Account not verified. Request OTP first.'], 403);
            }

            $user->tokens()->delete(); // Single session
            $token = $user->createToken('mobile-app');
            $plainTextToken = $token->plainTextToken;

            Log::info('User logged in successfully', ['phone' => $phone]);

            return response()->json([
                'token' => $plainTextToken,
                'user'  => $this->userResource($user),
            ]);
            
        } catch (ValidationException $e) {
            Log::error('Validation error in login:', $e->errors());
            return response()->json(['message' => 'Validation failed', 'errors' => $e->errors()], 422);
        } catch (\Exception $e) {
            Log::error('Unexpected error in login:', [
                'message' => $e->getMessage(),
                'file' => $e->getFile(),
                'line' => $e->getLine(),
                'trace' => $e->getTraceAsString()
            ]);
            return response()->json(['message' => 'Server error. Please try again.'], 500);
        }
    }

    /**
     * POST /api/v1/auth/logout
     */
    public function logout(Request $request)
    {
        try {
            $request->user()->currentAccessToken()->delete();
            Log::info('User logged out', ['user_id' => $request->user()->id]);
            return response()->json(['message' => 'Logged out.']);
        } catch (\Exception $e) {
            Log::error('Error in logout:', [
                'message' => $e->getMessage(),
                'file' => $e->getFile(),
                'line' => $e->getLine()
            ]);
            return response()->json(['message' => 'Server error during logout.'], 500);
        }
    }

    private function userResource(User $user): array
    {
        return [
            'id'                      => $user->id,
            'phone'                   => $user->phone,
            'name'                    => $user->name,
            'subscription_status'     => $user->subscription_status,
            'subscription_expires_at' => $user->subscription_expires_at?->toISOString(),
        ];
    }
}
