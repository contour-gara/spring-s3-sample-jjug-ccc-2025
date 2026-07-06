#!/bin/bash

awslocal s3 mb s3://jjug-ccc-2025
awslocal s3api put-bucket-cors --bucket jjug-ccc-2025 --cors-configuration '{"CORSRules": [{"AllowedHeaders": ["content-type"], "AllowedMethods": ["PUT"], "AllowedOrigins": ["http://localhost:3000"], "MaxAgeSeconds":3000}]}'
