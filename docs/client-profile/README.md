# Client Profile Onward Logistics

Onward Logistics is a fast-growing last-mile delivery company serving online merchants across West Africa. It connects merchants, riders, and customers, including live parcel tracking. Its engineering team is lean and growing, and its systems are built with Python and Java.

## Current AWS starting point

The company has one or more AWS accounts configured manually, a small number of servers, and inconsistent practices. An internal audit found that too many people had too much access.

## Business priorities

Leadership wants to grow without losing customer trust or control of costs. Priorities, in order:

1. **Grow without breaking:** handle peak days without outages.
2. **Protect customer and merchant data:** trust is central to the service.
3. **Control cost:** delivery margins are thin.
4. **Ship improvements faster:** move from monthly to weekly releases.
5. **Be ready for audits:** maintain evidence and consistent practices.

## Constraints

- A small platform team supports a much larger group of engineers.
- The budget is limited; unexplained spend is a problem.
- Data protection rules apply in every market where the company operates.
- Many customers use mobile connections that can be slow or unreliable.

## Standards for every solution

| Standard | Rule |
| --- | --- |
| Naming | Use `onward-environment-purpose`, for example `onward-dev-orders`. Environments are `dev`, `test`, and `prod`. |
| Tags | Tag supported resources with `Project=onward`, `Owner=<your name>`, and `Environment=<environment>`. |
| Access | Grant only the access people and systems need. Do not use shared accounts; avoid long-lived credentials. |
| Automation | Make infrastructure repeatable from code in Git wherever the lab allows. |
| Cost | Assign every resource an owner, delete what is no longer needed, and keep budget alerts active. |
| Region | Name and justify the AWS Region for each lab, or choose one when asked. |
| Decisions | Record each decision and explain its reason in business terms. |

## Earlier requirement from Week 1

Engineers must be onboarded safely and consistently. External people should receive only temporary, limited access. Changes to access must be traceable, and spend must be visible from day one.

## Editable source

The original Word document is available at [`../source/client-profile-needs.docx`](../source/client-profile-needs.docx).
