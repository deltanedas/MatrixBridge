![Github Workflows](https://github.com/deltanedas/matrix-bridge/workflows/Java%20CI/badge.svg)

### Matrix-Mindustry bridge

Bridges messages from matrix to mindustry and vice versa.

### Building a Jar

`$ make -j$(nproc)`

Output jar will be at `matrix-bridge.jar`.

### Installing

Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.

List your currently installed plugins/mods by running the `mods` command.

### Configuration
Get a matrix auth token from a client's http requests and put it in `matrix_token`.

Put your user id of form `@username:server` into `matrix_user`.

Put the bridged room's id of form `!id:server` into `matrix_room`.

Put your server of form `https://server` into `matrix_server`.
