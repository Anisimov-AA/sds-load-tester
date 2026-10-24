# Create ECR repository
resource "aws_ecr_repository" "app_repo" {
  name                 = var.ecr_repository_name
  image_tag_mutability = "MUTABLE"
  force_delete         = true # allow terraform destroy even when images are inside

  image_scanning_configuration {
    scan_on_push = false
  }

  tags = {
    Name        = var.ecr_repository_name
    Environment = "dev"
  }
}

# Output the repository URL
output "ecr_repository_url" {
  value = aws_ecr_repository.app_repo.repository_url
}
