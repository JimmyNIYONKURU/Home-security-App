# Home security system

## Overview

This is a java-based application designed to enhance home security by monitoring sensors.

## Features

- Sensor management for door, window, etc.
- Image processing for detecting cats and other specified criteria.
- Customizable alarm and arming statuses with associated behaviors.
- Notification system for status changes.

## Installation

1. Clone the repo to your local machine
2. Navigate to the project's root directory
3. Use Maven to compile and package the application
``` java
mvn clean package
````
4. Run the application using the java command:
```
java -jar target/security-system-1.0-SNAPSHOT.jar
```

## Adding and managing sensors

- Sensors can be added through the GUI specifying their type.
- Sensor statuses can be toogled to simulate activation or deactivation.

## Image processing

- Use the "Refresh Camera" button to simulate the camera feed with your image.
- The "Scan Picture" button sends the current image for analysis, detecting specified patterns or animals.

## Testing

Unit tests only cover core functionalities and can be run with Maven:
```
mvn test
```