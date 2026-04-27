<?php
// Create this middleware: app/Http/Middleware/LogAllRequests.php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

class LogAllRequests
{
    public function handle(Request $request, Closure $next)
    {
        // Log EVERY request that comes in
        Log::info('=== INCOMING REQUEST ===', [
            'method' => $request->method(),
            'url' => $request->fullUrl(),
            'headers' => $request->headers->all(),
            'body' => $request->all(),
            'ip' => $request->ip(),
            'user_agent' => $request->userAgent(),
            'content_type' => $request->header('Content-Type'),
            'accept' => $request->header('Accept'),
            'app_version' => $request->header('X-App-Version'),
            'authorization' => $request->header('Authorization') ? '[REDACTED]' : 'NONE',
            'timestamp' => now()->toISOString()
        ]);

        try {
            $response = $next($request);
            
            Log::info('=== RESPONSE ===', [
                'status' => $response->getStatusCode(),
                'timestamp' => now()->toISOString()
            ]);
            
            return $response;
        } catch (\Exception $e) {
            Log::error('=== REQUEST FAILED ===', [
                'error' => $e->getMessage(),
                'file' => $e->getFile(),
                'line' => $e->getLine(),
                'trace' => $e->getTraceAsString(),
                'timestamp' => now()->toISOString()
            ]);
            
            throw $e;
        }
    }
}

// Register this middleware in app/Http/Kernel.php
// Add to $middleware array (global middleware):
// \App\Http\Middleware\LogAllRequests::class,
