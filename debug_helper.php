<?php
// Add this to your app/Helpers/Functions.php or at the top of your controller

if (!function_exists('normalizePhone')) {
    function normalizePhone(string $phone): string
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
}

// Test endpoint to verify basic functionality
// Add this to your routes/api.php:
// Route::post('/auth/test', function () {
//     Log::info('Test endpoint hit at: ' . now());
//     return response()->json(['message' => 'Test successful', 'timestamp' => now()]);
// });
