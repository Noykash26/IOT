import rticonnextdds_connector as rti
from os import path as osPath
from time import sleep
filepath = osPath.dirname(osPath.realpath(__file__))

connector = rti.Connector("MyParticipantLibrary::Dashboard",  filepath + "/DDS.xml")
camera_input_DDS = connector.getInput("DashboardSubscriber::Dashboard_camera_reader")
temp_input_DDS = connector.getInput("DashboardSubscriber::Dashboard_temp_reader")
actuator_input_DDS = connector.getInput("DashboardSubscriber::Dashboard_actuator_reader")

cameraString = ''
# list of 10 last states for each participant
temp1List = list()
temp2List = list()
actuator1List = list()
actuator2List = list()

while True:
    #  Camera
    camera_input_DDS.take()
    numOfSamples_camera = camera_input_DDS.samples.getLength()
    if numOfSamples_camera > 0:
        if camera_input_DDS.samples[numOfSamples_camera - 1].valid_data:
            cameraString = camera_input_DDS.samples[numOfSamples_camera - 1].get_string("String")  # last data from camera

    #  Actuators 1 and 2
    actuator_input_DDS.read()
    numOfSamples_actuator = actuator_input_DDS.samples.getLength()
    actCounter1 = 0
    actCounter2 = 0
    for j in range(numOfSamples_actuator-1,0,-1):
        if actuator_input_DDS.infos.isValid(j):
            if actuator_input_DDS.samples.getString(j, "ActuatorID") == '314963810' and actCounter1 < 10:
                status1 = actuator_input_DDS.samples.getString(j, "ActuatorStatus")
                actuator1List.append(status1)
                actCounter1 += 1
            if actuator_input_DDS.samples.getString(j, "ActuatorID") == '313360489' and actCounter2 < 10:
                status2 = actuator_input_DDS.samples.getString(j, "ActuatorStatus")
                actuator2List.append(status2)
                actCounter2 += 1
        if actCounter1 == 10 and actCounter2 == 10 :
            break

    # Temp Sensors 1 and 2
    temp_input_DDS.read()
    numOfSamples_temp = temp_input_DDS.samples.getLength()
    tempCounter1 = 0
    tempCounter2 = 0
    for j in range(numOfSamples_temp-1,0,-1):
        if temp_input_DDS.infos.isValid(j):
            if temp_input_DDS.samples.getString(j, "TempID") == '1' and tempCounter1 < 10:
                temp1 = temp_input_DDS.samples.getNumber(j, "TempNumber")
                if temp1 < 20 or temp1 > 40:  # extreme temperature in sensor 1
                    temp1List.append(temp1)
                    tempCounter1 += 1

            if temp_input_DDS.samples.getString(j, "TempID") == '2' and tempCounter2 < 10:
                temp2 = temp_input_DDS.samples.getNumber(j, "TempNumber")
                if temp2 > 40:  # extreme temperature in sensor 2
                    temp2List.append(temp2)
                    tempCounter2 += 1
        if tempCounter1 == 10 and tempCounter2 == 10 :
            break

    #  newest data is in last cell on the list
    actuator1List.reverse()
    actuator2List.reverse()
    temp1List.reverse()
    temp2List.reverse()

    #  Dashboard output
    print(f'Camera: <{cameraString}>')
    print(f'Actuator 1: {actuator1List}')
    print(f'Actuator 2: {actuator2List}')
    print(f'Extreme Temp.1: {temp1List}')
    print(f'Extreme Temp.2: {temp2List}')
    print()

    #  Clear list for next iteration
    actuator1List.clear()
    actuator2List.clear()
    temp1List.clear()
    temp2List.clear()

    sleep(5)




