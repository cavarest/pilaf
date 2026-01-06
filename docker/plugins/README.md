# Custom Plugins Directory

Place your custom plugin JAR files in this directory. They will be mounted to the PaperMC container's `/plugins/custom` directory and loaded alongside any base image plugins.

## Usage

1. Copy your plugin JAR files to this directory
2. Restart the PaperMC container with `docker-compose restart papermc`

## Notes

- The itzg/minecraft-server image loads plugins from `/plugins` directory
- Base image plugins are in `/plugins` inside the container
- Custom plugins from this directory are mounted to `/plugins/custom`
- Both sets of plugins are loaded at server startup
- Do NOT mount a volume directly to `/plugins` as this will shadow base plugins
