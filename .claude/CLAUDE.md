# Project rules for Claude

## Commits and pull requests
- **No Claude trailer.** Never add `Co-Authored-By: Claude ...`, "Generated with Claude Code", or any other AI attribution to commit messages, PR titles, or PR descriptions.
- Use Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, scoped like `feat(infra):`), matching the existing history.
- Commit or push only when explicitly asked. Never commit directly to `main`; work on a feature branch.
- Never skip hooks (`--no-verify`) or force-push.

## Ask before acting
Stop and ask the user before doing any of the following:
- Committing, pushing, merging, rebasing, resetting, switching branches, stashing, or tagging.
- Deleting or overwriting files or folders (including `infrastructure/` templates and `docs/`).
- Running any `aws` command (CloudFormation deploy/delete, ECR push, ECS/CodeDeploy changes) — these cost money or affect live resources.
- Building/pushing Docker images to a registry.
- Changing `app/taskdef.json`, `app/appspec.yaml`, `app/build-and-push-ecr.yml`, or CloudFormation parameters (account IDs, ARNs, regions, ports, CPU/memory).
- Adding or upgrading dependencies.
- Anything involving secrets, credentials, `.env` files, or IAM permissions.
- Creating GitHub PRs, releases, or posting comments.
- When requirements are ambiguous or a change goes beyond what was asked.

## General
- Never hardcode secrets, account IDs, or credentials; use parameters or placeholders.
- Keep README/docs in sync when infrastructure or app behaviour changes.
- Report outcomes honestly: if a build or test fails, say so with the output.
