# Week 3 Lab 1 Practical Solution

Java Spring Boot full-stack order demo and CloudFormation infrastructure for the Onward Logistics Lab 1.

## Project layout

- `app/` contains the browser UI and Java REST backend.
- `infrastructure/templates/` contains the current EC2/NAT draft, which is marked legacy and must not be deployed; see its README for the required replacement stack flow.
- `app/` contains the container build, CodeDeploy task definition/AppSpec inputs, and GitHub Actions workflow source.
- `docs/architecture/` contains the required Fargate and blue/green architecture flow.
- `docs/decisions/` will record design choices and reasons.
- `docs/cost/` will hold the resource inventory and quiet-day/peak-day AWS Pricing Calculator assumptions.
- `scripts/load/` will hold code for the secure scaling demonstration.

All AWS resources will be defined in CloudFormation and deployed from version control using CloudFormation GitSync. The cost calculator will be used for estimates; its resource and usage assumptions will be kept in `docs/cost/`.

See `app/README.md` for local app instructions and API behavior.

[Numbered CloudFormation learning steps](infrastructure/templates/README.md)

The repository is currently a combined learning workspace on a feature branch. The client hand-in requires separate application and infrastructure repositories; split and publish those after the infrastructure templates are updated. Current CloudFormation templates are not deployment-ready for the lab requirements.
## Documentation

- [Lab 1 documentation index](docs/README.md)
- [Client profile and needs](docs/client-profile/README.md)
- [Completed discovery report](docs/discovery-report/README.md)
