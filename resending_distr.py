import csv


def make_dict():
    with open("../Data/log_ARQPackets.csv", mode='r') as csv_file:
        csv_reader = csv.DictReader(csv_file, fieldnames=["Packet","Response time","Times requested","Time elapsed"])
        next(csv_reader, None)
        resending = {}
        for row in csv_reader:
            if row["Times requested"] in resending:
                resending[row["Times requested"]] += 1
            else:
                resending[row["Times requested"]] = 1
    return resending

def write_csv(resending):
    with open("../Data/resending_distr.csv", mode='w+') as csv_file:
        csv_writer = csv.DictWriter(csv_file, fieldnames=["Times requested","Frequency"])
        csv_writer.writeheader()
        for key, value in resending.items():
            print(key, value)
            rowdict = {}
            rowdict["Times requested"] = key
            rowdict["Frequency"] = value
            csv_writer.writerow(rowdict)

if __name__ == "__main__":
    
    dict = make_dict()
    write_csv(dict)
