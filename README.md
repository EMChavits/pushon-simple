# PushOn Simple

PushOn Simple is a lightweight Hubitat app and driver for sending notifications via the Pushover service.

It is designed to provide **clear, predictable, and reliable** notifications for everyday automations, using Hubitat’s standard action:

PushOn Simple is intentionally opinionated.  
It prioritises clarity, guardrails, and long-term stability over flexibility or advanced customisation.

This project is shared publicly **as-is**, without support or ongoing maintenance.

---

## Overview

PushOn Simple focuses on a small, well-defined problem:

- Sending notifications from Hubitat to Pushover
- Supporting fixed notification priorities, including Emergency alerts
- Keeping behaviour explicit, readable, and easy to reason about

It is designed for **“set and forget”** operation, with strong defaults and safeguards to prevent accidental misuse.

---

## Design model

PushOn Simple uses a deliberately simple and explicit model:

- A **single Pushover user** (one User Key)
- **Four fixed notification devices**, mapped directly to Pushover priorities:
  - Low (priority -1)
  - Normal (priority 0)
  - High (priority 1)
  - Emergency (priority 2, repeating)
- No per-message configuration
- No custom actions
- No parameterised commands

Each device represents a clear, fixed behaviour.  
Priority selection is made by choosing the device, not by configuring individual messages.

---

## Scope

PushOn Simple is intended for users who:

- Use the Hubitat Elevation platform
- Already have a working Pushover account
- Use Rule Machine for automations
- Want simple, explicit notification behaviour
- Prefer reliability and predictability over feature richness

Typical use cases include:

- Routine informational alerts
- Important status notifications
- Safety-critical alerts that repeat until acknowledged

---

## Intentional limitations

PushOn Simple is intentionally limited by design.

It does **not** attempt to provide:

- Per-message priority selection
- Per-message retry or expiry configuration
- Message templating or advanced formatting
- Sound, quiet-hours, or presentation control
- Multiple Pushover users or routing
- Integration with services other than Pushover

These limitations exist to keep notifications predictable, understandable, and easy to maintain over time.

If you require highly customised or dynamic notification behaviour, PushOn Simple may not be the right tool for your use case.

---

## Installation

Installation consists of:

1. Installing the PushOn Simple App
2. Installing the PushOn Simple notification driver
3. Configuring Pushover credentials in the App
4. Using the generated notification devices in your automations

Detailed, step-by-step Hubitat UI guidance is intentionally not provided and assumes familiarity with standard Hubitat app and driver installation workflows.

---

## Emergency behaviour

The Emergency notification device uses repeating notifications until acknowledged in the Pushover app.

Default retry and expiry timings are provided and are suitable for most users.

An **optional expert-only override** is available for adjusting Emergency retry and expiry timings.  
This is disabled by default and protected by explicit confirmation and validation safeguards.

Incorrect values may cause Emergency notifications to repeat too frequently or stop sooner than expected.

---

## Support & maintenance

No support is provided for PushOn Simple.

There is no commitment to:

- Answer questions
- Provide troubleshooting assistance
- Fix bugs
- Add features
- Maintain compatibility with future Hubitat firmware versions
- Maintain compatibility with future Pushover service changes

If the code does not meet your needs, you are encouraged to fork the repository and maintain your own copy.

Use of this software is entirely at your own risk.

---

## Licence

PushOn Simple is released under the MIT License.  
See the `LICENSE` file for details.
