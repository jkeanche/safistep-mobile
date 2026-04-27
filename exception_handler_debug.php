<?php
// Add this method to your app/Exceptions/Handler.php

public function report(Throwable $e)
{
    // Log ALL exceptions with full details
    Log::error('Exception caught:', [
        'message' => $e->getMessage(),
        'file' => $e->getFile(),
        'line' => $e->getLine(),
        'trace' => $e->getTraceAsString(),
        'request' => request()->all(),
        'headers' => request()->headers->all(),
        'ip' => request()->ip(),
        'user_agent' => request()->userAgent()
    ]);
    
    parent::report($e);
}

public function render($request, Throwable $e)
{
    // Log rendering errors too
    Log::error('Rendering exception:', [
        'message' => $e->getMessage(),
        'file' => $e->getFile(),
        'line' => $e->getLine()
    ]);
    
    return parent::render($request, $e);
}
