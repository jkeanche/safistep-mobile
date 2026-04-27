#!/usr/bin/env python3
import requests
import json

# Test the OTP endpoint
url = "https://safistep.codejar.co.ke/api/v1/auth/request-otp"
headers = {
    "Content-Type": "application/json",
    "Accept": "application/json"
}

data = {
    "phone": "254712345678",
    "purpose": "registration"
}

try:
    print(f"Testing endpoint: {url}")
    print(f"Request data: {json.dumps(data, indent=2)}")
    
    response = requests.post(url, json=data, headers=headers, timeout=30)
    
    print(f"Status Code: {response.status_code}")
    print(f"Response Headers: {dict(response.headers)}")
    
    try:
        response_json = response.json()
        print(f"Response Body: {json.dumps(response_json, indent=2)}")
    except:
        print(f"Response Body (raw): {response.text}")
        
except requests.exceptions.RequestException as e:
    print(f"Request failed: {e}")
