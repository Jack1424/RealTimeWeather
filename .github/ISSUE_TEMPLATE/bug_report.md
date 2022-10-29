name: Bug report
about: Report problems/issues here
labels: bug
body:
- type: checkboxes
  attributes:
    label: Have you searched the wiki and looked for existing issues for this?
    description: Check the wiki first in case this is a common issue. If it isn't make sure that no one else has reported it yet.
    options:
    -  label: This is not a common issue or cannot be fixed by the solutions provided
       required: true
    -  label: This issue has not been reported yet
       required: true

- type: textarea
  attributes:
    label: What's happening
    description: Describe the issue that you're having. Make sure to be clear and concise.
  validations:
    required: true

- type: textarea
  attributes:
    label: What's supposed to happen
    description: Describe what should be happening
  validations:
    required: true

- type: dropdown
  attributes:
    label: Server Software
    description: What type of server are you running RTW on?
    options:
      - Bukkit
      - Paper
      - Spigot
      - Other
  validations:
    required: true

- type: input
  attributes:
    label: Minecraft version
    description: What version of Minecraft is your server running?
    placeholder: Example: 1.19.2
  validations:
    required: true

- type: input
  attributes:
    label: Server log
    description: If applicable, paste the link to your server log here
    placeholder: Get a link at https://pastebin.com/
  validations:
    required: false

- type: input
  attributes:
    label: RTW configuration
    description: If applicable, paste the link to your configuration file here **(REMEMBER TO REMOVE YOUR API KEY)**
    placeholder: Get a link at https://pastebin.com/
  validations:
    required: false

- type: textarea
  attributes:
    label: Additional information
    description: Put any more information (including screenshots and videos) that you might have here
  validations:
    required: false
