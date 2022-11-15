from sys import path as sysPath
import rticonnextdds_connector as rti
from os import path as osPath
from time import sleep
import random
filepath = osPath.dirname(osPath.realpath(__file__))

connector = rti.Connector("MyParticipantLibrary::TempSensor", filepath + "/DDS.xml")
outputDDS = connector.getOutput("TempPublisher::Temp2_writer")

while True:
    randomTemp2 = random.randint(0, 50)
    sensorID = '2'  # this is sensor number 2
    outputDDS.instance.setNumber("TempNumber", randomTemp2)
    outputDDS.instance.setString("TempID", sensorID)
    outputDDS.write()
    print(f'Sensor {sensorID} current temperature is: {randomTemp2}')

    sleep(0.1)

