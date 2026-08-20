# Local API stubs

These files are **not** part of the plugin. They are minimal, hand-written declarations of the
Bukkit/Paper API surface that ScriptedEssentials touches, so `tools/apicheck/check.sh` can run
`javac` over `src/main/java` in an environment that cannot reach `repo.papermc.io`.

They type-check this project's own code — syntax, generics, control flow, overrides, dead code.
They are **not** a substitute for building against the real `paper-api`: run `mvn package` for
that. If a stub signature ever disagrees with the real API, the real API is right and the stub
should be corrected.
