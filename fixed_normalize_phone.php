<?php
// FIXED normalizePhone function - replace the one in your controller

private function normalizePhone(string $phone): string
{
    // Fix 1: Correct regex syntax - add proper delimiters
    $cleaned = preg_replace('/\s+/', '', trim($phone));
    $cleaned = ltrim($cleaned, '+');
    
    // Fix 2: Use proper string functions (str_starts_with is PHP 8+)
    if (str_starts_with($cleaned, '254')) {
        return $cleaned;
    }
    
    if (str_starts_with($cleaned, '0')) {
        return '254' . substr($cleaned, 1);
    }
    
    return '254' . $cleaned;
}

// OR if you're on PHP < 8, use this version:
private function normalizePhone(string $phone): string
{
    // Fix 1: Correct regex syntax
    $cleaned = preg_replace('/\s+/', '', trim($phone));
    $cleaned = ltrim($cleaned, '+');
    
    // Fix 2: Use strpos() for PHP < 8 compatibility
    if (strpos($cleaned, '254') === 0) {
        return $cleaned;
    }
    
    if (strpos($cleaned, '0') === 0) {
        return '254' . substr($cleaned, 1);
    }
    
    return '254' . $cleaned;
}

// The original problematic code was:
// $cleaned = preg_replace('\s+', '', trim($phone));  // MISSING / delimiters
// Should be:
// $cleaned = preg_replace('/\s+/', '', trim($phone));  // CORRECT with / delimiters
