#External module imports
import RPi.GPIO as GPIO
import sys

# Pin Definitions:
outPin = int(sys.argv[1])
state = bool(sys.argv[2])


# Pin Setup:
GPIO.setmode(GPIO.BOARD) # Broadcom pin-numbering scheme
GPIO.setup(outPin, GPIO.OUT)

# Initial state for LEDs:
if state:
    GPIO.output(outPin, GPIO.HIGH)
else:
    GPIO.output(outPin, GPIO.LOW)

GPIO.cleanup()
