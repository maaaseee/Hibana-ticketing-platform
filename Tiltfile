# Tiltfile for Hibana.tk Ticketing Platform
# Runs complete containerized microservices stack via Docker Compose with dedicated Web UI

# 1. Instruct Tilt to manage Docker Compose
docker_compose('docker-compose.yml')

# 2. Configure Tilt resources with clear categorization
dc_resource('frontend', labels=['frontend'])
dc_resource('gateway', labels=['gateway'])
dc_resource('catalog', labels=['microservices'])
dc_resource('inventory', labels=['microservices'])
dc_resource('orders', labels=['microservices'])
dc_resource('mailpit', labels=['infrastructure'])
dc_resource('postgres', labels=['infrastructure'])
dc_resource('redis', labels=['infrastructure'])
dc_resource('floci-az', labels=['infrastructure'])
