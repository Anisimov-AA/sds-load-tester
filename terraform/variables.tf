variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "us-east-1"
}

variable "service_name" {
  description = "Name of the ECS service"
  type        = string
  default     = "product-service"
}

variable "ecr_repository_name" {
  description = "Name of the ECR repository"
  type        = string
  default     = "product-service"
}

variable "desired_count" {
  description = "Number of tasks to run"
  type        = number
  default     = 1
}

variable "container_port" {
  description = "Port the container listens on"
  type        = number
  default     = 8080
}
