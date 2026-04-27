<?php
// Check your config/cors.php - it should look like this:

return [
    'paths' => ['api/*', 'sanctum/csrf-cookie'],
    'allowed_methods' => ['*'],
    'allowed_origins' => ['*'], // Allow all origins for debugging
    'allowed_origins_patterns' => [],
    'allowed_headers' => ['*'], // Allow all headers
    'exposed_headers' => [],
    'max_age' => 0,
    'supports_credentials' => false,
];

// Also check app/Http/Kernel.php - make sure HandleCors is in middleware stack:

protected $middleware = [
    // ... other middleware
    \Fruitcake\Cors\HandleCors::class,
    // ... other middleware
];

protected $middlewareGroups = [
    'api' => [
        // ... other middleware
        \Fruitcake\Cors\HandleCors::class,
        // ... other middleware
    ],
];
