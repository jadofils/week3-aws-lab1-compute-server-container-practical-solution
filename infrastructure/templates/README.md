# Numbered CloudFormation learning steps

These files are draft CloudFormation learning templates with block comments explaining the major sections. They show how the infrastructure stacks are intended to connect. The parameter and output names, S3 publishing path, artifact publishing process, and cost assumptions still need to be reconciled before deployment.

## How the numbered flow works

1. `0-artifact-store-bootstrap.yaml` is a small independent stack. It creates the private S3 store needed for nested templates and the app package.
2. A code-based publishing step uploads the child templates to a versioned S3 prefix. A change to a child template must also cause GitSync to see a change to its configured template or deployment file.
3. `1-main-root-stack.yaml` is the root template. It holds shared parameters and tags, then creates the child stacks.
4. Child templates `2` through `6` define network, security/IAM, load balancing, compute/scaling, and operations. The root passes outputs from earlier children into later ones.

The filenames show the learning/dependency order. CloudFormation does not deploy files just because their names are numbered: the root template's nested-stack references and dependencies control stack creation. GitSync must be configured for the root template and its deployment file.

## Flow in pseudocode

```text
GitHub commit
  -> GitSync sees the configured root template or deployment-file change
  -> root stack reads shared parameters and tags
  -> root creates the network child stack
  -> root passes VPC and subnet outputs to security and load-balancer children
  -> root passes private subnets, app security group, IAM profile, and target group to compute child
  -> EC2 instances start the app and pass the /health check
  -> ALB sends customer requests to healthy app instances
  -> CloudWatch metrics drive Auto Scaling; failed instances are replaced
```

The numbered filenames show the learning and dependency order. Treat capacity values and other defaults as initial assumptions for the later AWS Pricing Calculator review.
