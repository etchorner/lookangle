# Where did I put that damned satellite? #

This is an Android app (apologies to all real Android apps) which will allow the user to calculate the azimuth, vertical angle, and LNB skew needed to properly install and align a directional satellite communication antenna. Whether you are a DIY installer, professional dish dood, or a military SATCOM-on-the-move operator, this may be useful to your world.

You have to admit, an Android look angle app is sexier than whipping out the old polar scale slide rule....and if I get the math correct, it'll be a darned sight more accurate.

## Design Approach ##

There are two approaches internally used for the look angle calculations:

  1. Simple spherical trigonometry commonly in use. See [Table Top Physics](http://tinyurl.com/25tco7o) for an explanation.
  1. A more rigorous ellipsoidal calculation proposed by Soler, T., & D.W. Eisemann (1994). For those interested, examine the source document at [Determination of look angles to geostationary communication satellites, J. Surv. Eng. ASCE, 120(3), 115-127](http://tinyurl.com/22lyakf) (PDF link). The authors admit that the ellipsoidal approach is not significantly more accurate (~2/100 of a degree or so), but may pay benefits for users who do require additional accuracy in look angle calculations. It's just a little more fun to try and implement something out of the ordinary.

Additionally, there are some coordinate transformation routines included for the eventual user selected options of UTM, geodetic L/L, and MGRS coordinate display and input.

## Future Movement ##
When I have free time (ha) I'm looking to add in some "bubble level" and "magnetic compass" functions to actually make the app a one-stop shop for antenna installation and alignment.


---

**Be advised: I ~~suck~~ underperform at UI design, so if you want pretty, you're going to have to contribute!**

---

