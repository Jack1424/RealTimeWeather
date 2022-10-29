name: Feature request
description: Use this to request new features and/or changes
labels: enhancement
body:
- type: checkboxes
  attributes:
    label: Is there already a request similar to this?
    description: Check the [other feature requests](https://github.com/Jack1424/RealTimeWeather/issues?q=label%3Aenhancement+) and ensure that this hasn't been requested yet
  options:
    -  label: There are and have not been any feature requests similar to this
       required: true

- type: textarea
  attributes:
    label: What's your suggestion?
    description: Use the space below to describe your suggestion. Feel free to attach any relevant files/links.
  validations:
    required: true