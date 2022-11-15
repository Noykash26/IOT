from sys import path as sysPath
import rticonnextdds_connector as rti
from os import path as osPath
from time import sleep
import random
filepath = osPath.dirname(osPath.realpath(__file__))

connector = rti.Connector("MyParticipantLibrary::Button", filepath + "/DDS.xml")
outputDDS = connector.getOutput("ButtonPublisher::Button_writer")

while True:
    buttonState = 'Start'  # default state
    outputDDS.instance.setString("String", buttonState)
    outputDDS.write()
    print(f'Button commend: {buttonState}')
    sleep(20)

    buttonState = 'Stop'
    outputDDS.instance.setString("String", buttonState)
    outputDDS.write()
    print(f'Button commend: {buttonState}')

    sleep(5)

