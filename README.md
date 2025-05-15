# ShadyBeam: Cyber Defense Project

## Project Overview

ShadyBeam is a research project demonstrating potential attack vectors for mobile device exploitation. The system consists of two components that simulate a covert data exfiltration scenario: a seemingly innocent Android flashlight application and a backend server that collects device information and images.

This project is intended for **educational purposes only** as part of a cyber defense course, to demonstrate how malicious actors might disguise data collection within seemingly benign applications.

## System Architecture

### Android Application Component

The Android application presents itself as a standard flashlight utility with the following characteristics:

- Simple user interface with a flashlight toggle button
- Requires camera, flashlight, and internet permissions (commonly requested by legitimate flashlight apps)
- When the user toggles the flashlight, the application covertly:
  - Captures an image using the device camera
  - Encodes the image in Base64 format
  - Transmits the image along with device metadata to a remote server

### Server Component (Upload Controller)

The server component receives and processes data transmitted from the Android application:

- Provides a RESTful API endpoint for receiving uploads
- Processes incoming data and extracts metadata
- Stores captured images and device information
- Logs all activities and data transmissions

## Social Engineering Aspects

This project demonstrates several social engineering techniques commonly employed in malicious applications:

### Trust Exploitation

The application exploits user trust by:

- Presenting a legitimate and expected interface (flashlight functionality)
- Requesting permissions that seem reasonable for the stated purpose
- Performing the advertised functionality correctly while hiding secondary operations

### Permission Abuse

The application demonstrates permission abuse through:

- Utilizing camera permissions ostensibly needed for flashlight functionality
- Leveraging internet permissions under the guise of "analytics" or "updates"
- Executing covert operations during expected application activities

### Data Exfiltration Techniques

The application showcases methods for covert data extraction:

- Timing sensitive operations to coincide with user-initiated actions
- Encoding data to avoid detection by simple network monitoring
- Using standard HTTP protocols that blend with normal traffic
- Employing legitimate-appearing server communication

## Setup and Usage
### Android Application

1. Open the android-app directory in Intellij (probably easiest)
2. Configure the `API_ENDPOINT` to point to your server instance
3. Build and install on a device, intellij has easy access with android plugin

### Server Component

1. Navigate to the upload-controller directory
2. Configure server settings in the application configuration files
3. Run using Gradle: `./gradlew run`
4. 