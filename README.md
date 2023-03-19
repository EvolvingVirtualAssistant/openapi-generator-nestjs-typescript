# OpenApi Generator Nest.js TypeScript

This project consists of a custom template for the openapi-generator lib (https://github.com/OpenAPITools/openapi-generator) for Nest.js TypeScript server projects.
This is a very simple 'quick and dirty' version, and it was built based on the needs that we have.

We are currently working on supporting the generation of:
- Controllers with:
  - Nest.js decorators for REST API request handling
  - Payload validation
  - Nest.js swagger decorators to document an API  
- DTOs with:
  - Nest.js swagger decorators to document an API
  - DTOs schema validation  