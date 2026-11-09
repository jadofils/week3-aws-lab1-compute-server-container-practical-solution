LAB: Handling Sales Day Demand for the Onward Logistics Order System 

Module: Compute & Delivery on AWS  |  Week (M) 3 

1. Business Scenario 

Onward Logistics' order system now runs inside a proper network. Engineers have consolidated it into a single full stack application: one deployable that serves the user interface and processes orders together. On a normal day it handles the load comfortably. On sales days, order volume triples within hours, and last year slow pages and errors at the peak cost the company merchants and customer trust. Running enough capacity for the peak all month would be expensive, and nobody wants to adjust capacity by hand in the middle of a sales event. 

In a planning meeting, four leaders set out what they need: 

Chief Technology Officer: “On a very busy sales day the order system has to automatically adjust compute workload to handle volumes of incoming traffic when cpu of servers spike without anyone touching it, and when the rush is over we should not keep paying for the extra capacity.” 

Head of Security: “Customers should reach the order system through one public entry point and nothing else. The servers that run it must never be reachable directly from the internet, and nobody should be opening admin ports to fix them.” 

Head of Operations: “If a server dies, or a whole zone of the data center goes down, customers should not notice and I should not be woken up to fix it. I also need to see which servers are answering as capacity changes.” 

Finance Lead: “Show me what this design costs on a quiet day and on a peak day, and why you chose it.” 

For background on the company, its stakeholders and its standards, read the Client Profile: Onward Logistics. 

2. What the Client Expects 

These are the objectives. How you achieve them is your design. 

Give merchants and customers a single, always available public entry point to the order application, however many servers run it. 

Make the application handle sales day demand automatically, and return to a lean size afterwards. 

Keep the servers that run the application private, while still letting them download updates from the internet. 

Keep the application working through the loss of a server or an entire Availability Zone, without manual action. 

Let operations see which server answered a request as capacity changes. 

Show the Finance Lead what the design costs and justify your choices on cost and resilience. 

Prove to the CTO, the Head of Security and the Finance Lead that every objective is met. 

3. Mandatory Technical Requirements 

Provision everything with AWS CloudFormation. Nothing may be created manually in the console. 

Deploy the template using CloudFormation GitSync from a version controlled repository. 

Ensure the infrastructure is configured to be highly available and strictly matches the requirements of the Head of Security. 

Build the order application in the language your assigned stack (Python or Java). It must be a single full stack application, with the user interface and the order logic served together. 

Run the application on Free Tier eligible Amazon Linux servers ec2 instances in private subnets, behind an internet facing Application Load Balancer. The application must be installed and started automatically when a server launches, and must display a page showing your full name, the lab name and which server answered. 

Give private servers outbound internet access through a Regional NAT Gateway. 

Capable of trigger a spike in workload securely via a ui interface or secure remote access to demonstrate automatic adjustment of compute resources to handle additional load. 

Follow the company standards in the Client Profile, including naming and tags. 

Do not use inline policies anywhere in the template. 

4. Stage 1: Discovery (Before You Build) 

Complete a discovery report before you build anything, using the Discovery Report Template. Fill it in and share the link when you submit. It has two parts: 

What you found: common solutions that exist for this kind of problem, and what happens with no solution. 

Business value: your chosen approach, the AWS services you plan to use and why, and who benefits. 

Your research should help you decide how to meet the objectives above. 

5. Stage 2: Build and Prove 

Design and build a solution that meets the objectives and the mandatory technical requirements. You decide the design, including how many servers you need and their limits, what triggers a change in capacity, the security rules, how the application gets onto each server, and how servers get what they need. Create an architecture diagram of your solution. Be ready to demonstrate the client's checks live. 

The client will check that: 

The application is reached through one public address and stays reachable as servers come and go. 

Requests are shared across servers, and the page shows which server answered. 

Rising demand makes the number of servers grow automatically, and the number shrinks again afterwards. 

A failed server is replaced without manual action. 

The application keeps working if one Availability Zone is lost. 

Only the load balancer is public: no one on the internet can reach a server directly, and servers can still download updates. 

You can explain what your design costs and which alternatives you rejected. 

The whole setup can be rebuilt from the repository. 

6. Deliverables 

Discovery report: a link to your completed report (what you found and business value). 

Lab deliverables: a link to the repository holding your CloudFormation template, and to the repository holding your application code if it is separate. 

Architecture diagram: a link to a diagram showing the resources deployed inside the VPC, which are public and which are private, and how traffic flows through them. 

Live review: walk through your code, demonstrate the client's checks, and defend your reasoning. 

7. Rubric 

Discovery report (what you found and business value): 20% 

Solution meets the objectives and technical requirements: 50% 

Architecture diagram: 10% 

Demonstrate understanding of solution in live review: 20% 

8. Submission 

Submission Type: Form link (discovery report link, repository links and architecture diagram link) 

Grading: Live Review Session 