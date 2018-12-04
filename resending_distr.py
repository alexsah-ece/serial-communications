import csv


def make_dict():
    with open("../Data/log_ARQPackets.csv", mode='r') as csv_file:
        csv_reader = csv.DictReader(csv_file, fieldnames=["Packet","Response time","Times requested","Time elapsed"])
        next(csv_reader, None)
        transmissions = {}
        for row in csv_reader:
            if row["Times requested"] in transmissions:
                transmissions[row["Times requested"]] += 1
            else:
                transmissions[row["Times requested"]] = 1
    return transmissions

def write_csv(transmissions):
    with open("../Data/transmissions_distr.csv", mode='w+') as csv_file:
        csv_writer = csv.DictWriter(csv_file, fieldnames=["Times requested","Frequency"])
        csv_writer.writeheader()
        for key, value in transmissions.items():
            print(key, value)
            rowdict = {}
            rowdict["Times requested"] = key
            rowdict["Frequency"] = value
            csv_writer.writerow(rowdict)

def calculate_BER(transmissions):
    total_transmissions = 0
    successful_packets = 0
    for key, value in transmissions.items():
        total_transmissions += float(key) * float(value)
        successful_packets += value
    P = successful_packets / total_transmissions
    print("P: %f" %(P))
    ber = 1 - P**(1/float(16*8))
    print("BER: %f" %(ber))
    #Pe = 1 - (1-Pp)**(1/)



if __name__ == "__main__":
    
    dict = make_dict()
    write_csv(dict)
    calculate_BER(dict)
