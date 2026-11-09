# Week 3 Lab 1 Practical Solution

Java Spring Boot full-stack order demo and CloudFormation infrastructure for the Onward Logistics Lab 1.

## Project layout

- `app/` contains the browser UI and Java REST backend.
- `infrastructure/` will contain the AWS CloudFormation template and parameters.
- `docs/architecture/` will contain the architecture diagram source.
- `docs/decisions/` will record design choices and reasons.
- `docs/cost/` will hold the resource inventory and quiet-day/peak-day AWS Pricing Calculator assumptions.
- `scripts/load/` will hold code for the secure scaling demonstration.

All AWS resources will be defined in CloudFormation and deployed from version control using CloudFormation GitSync. The cost calculator will be used for estimates; its resource and usage assumptions will be kept in `docs/cost/`.

See `app/README.md` for local app instructions and API behavior.
