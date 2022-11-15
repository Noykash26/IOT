from sys import path as sysPath
import rticonnextdds_connector as rti
from os import path as osPath
from time import sleep
from _datetime import datetime
filepath = osPath.dirname(osPath.realpath(__file__))

connector = rti.Connector("MyParticipantLibrary::Camera", filepath + "/DDS.xml")
outputDDS = connector.getOutput("CameraPublisher::Camera_writer")

while True:
    now = datetime.now()
    current_time = now.strftime("%H:%M:%S.%f")  # time format in millisecond
    outputDDS.instance.setString("String", current_time)
    outputDDS.write()
    print(f'Current state is: {current_time}')
    sleep(0.1)
