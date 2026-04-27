<?php
// Add these test routes to your routes/api.php file

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

// Test basic Laravel functionality
Route::get('/test', function () {
    Log::info('Basic test endpoint hit');
    return response()->json([
        'message' => 'Laravel is working',
        'timestamp' => now()->toISOString(),
        'environment' => app()->environment()
    ]);
});

// Test POST request
Route::post('/test-post', function (Request $request) {
    Log::info('POST test endpoint hit', $request->all());
    return response()->json([
        'message' => 'POST request working',
        'received' => $request->all(),
        'timestamp' => now()->toISOString()
    ]);
});

// Test OTP endpoint without dependencies
Route::post('/auth/test-otp-simple', function (Request $request) {
    Log::info('Simple OTP test hit', $request->all());
    
    try {
        $request->validate([
            'phone' => 'required|string',
            'purpose' => 'string'
        ]);
        
        return response()->json([
            'message' => 'Test OTP successful',
            'phone' => $request->phone,
            'otp' => '123456', // Hardcoded for testing
            'timestamp' => now()->toISOString()
        ]);
    } catch (\Exception $e) {
        Log::error('Simple OTP test failed', [
            'error' => $e->getMessage(),
            'trace' => $e->getTraceAsString()
        ]);
        
        return response()->json([
            'error' => $e->getMessage(),
            'file' => $e->getFile(),
            'line' => $e->getLine()
        ], 500);
    }
});

// Add this to your existing AuthController temporarily:
public function simpleRequestOtp(Request $request)
{
    Log::info('Simple OTP request received', $request->all());
    
    try {
        $request->validate([
            'phone' => 'required|string',
            'purpose' => 'string'
        ]);
        
        return response()->json([
            'message' => 'OTP sent successfully',
            'phone' => $request->phone,
            'otp' => '123456' // Hardcoded for testing
        ]);
    } catch (\Exception $e) {
        Log::error('Simple OTP failed', [
            'error' => $e->getMessage(),
            'file' => $e->getFile(),
            'line' => $e->getLine()
        ]);
        
        return response()->json([
            'error' => $e->getMessage()
        ], 500);
    }
}

// Then add this route:
// Route::post('/auth/simple-otp', [AuthController::class, 'simpleRequestOtp']);
