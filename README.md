LIFX-Sunrise-Alarm
==================

An Android to interface with the LIFX light globe and schedule an alarm which simulates a Sunrise starting with dim red light then progressing through to bright white light.

This is intended to be used as a gentle wake-up helper, not as a replacement to a normal alarm sound.


Work in progress
================

I'm just throwing this up online in case anyone wants to use the code, especially the Java library for controlling the LIFX globe. This is not intended to become an actual product unless I put in a lot more work. So far it works enough for my own use so I've mostly stopped working on it, but I would welcome any patches and if someone wants to continue the work to support multiple globes or polish it up, that'd be more than welcome and I'm keen to accept pull requests. :)


Potential future enhancements:

 * Currently only expects there to be one LIFX globe (I have only one) so it will affect only whichever globe is the gateway.
 * The alarm does not repeat daily, you have to set it every time you want it to happen.
 * Does not re-set the alarm if the phone reboots
