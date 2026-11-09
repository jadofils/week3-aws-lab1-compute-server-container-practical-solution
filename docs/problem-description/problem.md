LAB: Delivering Order System Updates Safely for Onward Logistics 
Module: Compute & Delivery on AWS  |  Week (M) 3 
1. Business Scenario 
The Onward Logistics order system now runs as a single containerized full stack application: the user interface and the order logic are packaged together in one image. Today, engineers release changes by hand: they build an image on a laptop, upload it, and update the running service. It is slow, error-prone, and on peak days nobody wants to touch production. Leadership wants every approved change to reach customers automatically, safely and without downtime. 
In a planning meeting, four leaders set out what they need: 
•	Chief Technology Officer: “Merging a change should be the only manual step. Customers should never see an outage because of a release, we should be able to go back quickly, and a new engineer should find their way around this setup without a guided tour.” 
•	Head of Security: “No long-lived cloud credentials in our repositories or pipelines. Customers reach the application through one public entry point only, and the application itself must have no path to the internet: traffic to AWS services stays on AWS's network.” 
•	Head of Operations: “The application has to keep working if a container fails or a zone is lost, and it has to grow on busy days without me adjusting anything.” 
•	Finance Lead: “Idle environments should not cost us money, and I want to understand what the private connectivity costs.” 
For background on the company, its stakeholders and its standards, read the Client Profile: Onward Logistics. 
2. What the Client Expects 
These are the objectives. How you achieve them is your design. 
1.	Run the order application as a containerized service that merchants and customers reach through a single public entry point, and that stays up if a container or an Availability Zone fails. 
2.	Make every approved change to the application reach customers automatically, with no one logging in to deploy. 
3.	Release new versions without downtime, and make a bad release easy to reverse. 
4.	Keep the application off the internet entirely, with no long lived credentials anywhere in the delivery process. 
5.	Let the service grow on busy days and shrink afterwards. 
6.	Make the setup easy for the next engineer to read, understand and change. 
7.	Prove to the CTO, the Head of Security and the Finance Lead that every objective is met. 
3. Mandatory Technical Requirements 
•	Provision all infrastructure with AWS CloudFormation. Nothing may be created manually in the console. 
•	Deploy the infrastructure using CloudFormation GitSync from a version controlled repository. 
•	Ensure the infrastructure is configured to be highly available and strictly matches the requirements of the Head of Security. 
•	Build the order application in the language per your assigned stack (Python or Java), as a single full stack application with the user interface and the order logic packaged together in one container image. It must display a page showing your full name and the lab name. 
•	Run the application on Amazon ECS with Fargate, with tasks in private subnets behind an internet facing Application Load Balancer. 
•	Use VPC endpoints instead of a NAT gateway for connectivity between the application and AWS services. 
•	Use GitHub Actions to build the application image and push it to Amazon ECR, authenticating to AWS with OIDC. 
•	Use Amazon EventBridge to trigger AWS CodePipeline, and deploy new versions to ECS with AWS CodeDeploy blue/green deployments. 
•	Keep application code and infrastructure code in separate repositories. 
•	Follow the company standards in the Client Profile, including naming and tags. 
•	Do not use inline policies anywhere in the infrastructure templates. 
4. Stage 1: Discovery (Before You Build) 
Complete a discovery report before you build anything, using the Discovery Report Template. Fill it in and share the link when you submit. It has two parts: 
•	What you found: common solutions that exist for this kind of problem, and what happens with no solution. 
•	Business value: your chosen approach, the AWS services you plan to use and why, and who benefits. 
Your research should help you decide how to meet the objectives above. 
5. Stage 2: Build and Prove 
Design and build a solution that meets the objectives and the mandatory technical requirements. You decide the design, including how your infrastructure code is organized, how many containers you need and their limits, what triggers a change in capacity, the security rules, and how each release is handed to the deployment. Create an architecture diagram of your solution. Be ready to demonstrate the client's checks live. 
The client will check that: 
•	The application is reached through one public address, and its containers run in private subnets with no route to the internet. 
•	Pushing a change to the application repository results in a new version running, with no manual steps. 
•	A release reaches customers without downtime, and a bad release can be reversed. 
•	No long lived credentials exist in the repositories or pipelines. 
•	The service adds containers under load and removes them afterwards. 
•	The service keeps working if a container fails or an Availability Zone is lost. 
•	A new engineer could read your repositories and understand how the pieces fit together. 
•	You can explain what your design costs, including the private connectivity, and which alternatives you rejected. 
•	The whole setup can be rebuilt from the repositories. 
6. Deliverables 
1.	Discovery report: a link to your completed report (what you found and business value). 
2.	Lab deliverables: links to the repositories holding your CloudFormation infrastructure and the repository holding your application code and related files. 
3.	Architecture diagram: a link to a diagram showing the resources deployed inside the VPC, the delivery pipeline, and how a change flows work. 
4.	Live review: walk through your code, demonstrate the client's checks, and defend your reasoning. 
7. Rubric 
•	Discovery report (what you found and business value): 20% 
•	Solution meets the objectives and technical requirements: 50% 
•	Architecture diagram: 10% 
•	Demonstrate understanding of solution in live review: 20% 
8. Submission 
Submission Type: Form link (discovery report link, repository links and architecture diagram link) 
Grading: Live Review Session 

