<?php
// Add this to routes/api.php - this should ALWAYS work

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

Route::any('/debug', function (Request $request) {
    // This will log regardless of method, headers, or body
    Log::info('DEBUG ENDPOINT HIT', [
        'method' => $request->method(),
        'url' => $request->fullUrl(),
        'headers' => $request->headers->all(),
        'body' => $request->all(),
        'ip' => $request->ip(),
        'user_agent' => $request->userAgent(),
        'content_type' => $request->header('Content-Type'),
        'content_length' => $request->header('Content-Length'),
        'timestamp' => now()->toISOString()
    ]);
    
    return response()->json([
        'message' => 'Debug endpoint working!',
        'received' => [
            'method' => $request->method(),
            'headers' => $request->headers->all(),
            'body' => $request->all(),
            'timestamp' => now()->toISOString()
        ]
    ]);
});

// Test this endpoint from the app by temporarily changing the BASE_URL in build.gradle.kts:
// buildConfigField("String", "BASE_URL", "\"https://safistep.codejar.co.ke/api/v1/debug\"")
