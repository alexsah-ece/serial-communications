import java.lang.System.*;
import java.io.*;
import java.lang.*;
import java.util.Arrays;

class VirtualModem{

   static final String ECHO_REQUEST_CODE = "E6385\r";
   static final String IMAGE_REQEST_CODE = "M5550\r";
   //static final String IMAGE_REQEST_CODE = "M5841CAM=PTZ3\r";
   static final String IMAGE_REQEST_CODE_ERRORS = "G8167\r";
   static final String GPS_REQUEST_CODE_PACKETS = "P3157R=1087740\r";
   static final String GPS_REQUEST_CODE = "P3157";
   static final String ACK = "Q5059\r";
   static final String NACK = "R3674\r";


	private Modem modem;
	private final long duration;

	public VirtualModem(){

		modem = new Modem();
		modem.setSpeed(16000);
		modem.setTimeout(3000);
		duration = 5 * 60 * 1000;

		int k;

		modem.open("ithaki");


		while (true){
			try{

				k = modem.read();

				if (k == -1) break;	

				System.out.print((char) k);

			}catch(Exception x){

				System.out.println("Exception occured");
				break;
			}

		}

	}


	public void echoPackets() throws IOException{

		System.out.println("Receiving packets...\n");

		BufferedWriter txt = new BufferedWriter(new FileWriter("../Data/text_echoPackets.txt"));
		BufferedWriter log = new BufferedWriter(new FileWriter("../Data/log_echoPackets.csv"));

        log.write("Packet,Response time,Time elapsed");
        log.newLine();

		String packet = "";
		long st, start = System.currentTimeMillis();
        int count = 0;

		l1:
		while (System.currentTimeMillis()-start < duration){

			st = System.currentTimeMillis();
			modem.write(ECHO_REQUEST_CODE.getBytes());

			for(int i=0; i < 35; i++){

				int k = modem.read();
				if (k == -1) break l1;
				packet += (char)k;
			}
            count += 1;

			txt.write(packet);
			txt.newLine();
			packet = "";

			log.write(String.valueOf(count) + ",");
            log.write(String.valueOf((System.currentTimeMillis()-st)) + ",");
            log.write(String.valueOf((System.currentTimeMillis()-start)));
            log.newLine();      		
		}

		txt.flush();
		txt.close();

		log.flush();
		log.close();

		System.out.println("echoPackets END\n");
	}

	public void imagePackets(String code, String filename) throws IOException{

		System.out.println("Receiving image...\n");

		OutputStream image = new FileOutputStream("../Pictures/"+filename+".jpeg");
		
		int prev, cur;

		modem.write(code.getBytes());

		prev = modem.read();
		cur = modem.read();

		if (prev == 0xFF && cur == 0xD8){

			image.write(prev);

		}else{

			System.out.println("Connection not established");
			return;
		}

		while(cur != -1 ){

			image.write(cur);

			if(prev == 0xFF && cur == 0xD9){	
				break;
			}else{

				prev = cur;
				cur = modem.read();
			}
		}
	
		image.flush();
		image.close();

		System.out.println("imagePackets END\n");
	}

	public void gpsPackets() throws IOException{

		System.out.println("Receiving GPS...\n");

		BufferedWriter temp_w = new BufferedWriter(new FileWriter("../Data/temp_gps.txt"));
		BufferedReader temp_r = new BufferedReader(new FileReader("../Data/temp_gps.txt"));

		modem.write(GPS_REQUEST_CODE_PACKETS.getBytes());

		while (true){

			int k = modem.read();
			System.out.print(k);
			if(k == -1) break;
			temp_w.write((char)k);
		}

		temp_w.flush();
		temp_w.close();

		String[] packets = new String[60];
		temp_r.readLine();
		for(int i=0; i < 60; i++) packets[i] = temp_r.readLine();
		temp_r.close();

		//Pick four and combine them with the req_code to form the gps-image code
		String T = GPS_REQUEST_CODE;
		for(int i=0; i < 4; i++){
			System.out.println(Arrays.toString(packets));
			String[] temp = packets[i*10].split(",");
            System.out.println(Arrays.toString(temp));
			int longtitude = Integer.parseInt(temp[4].split("\\.")[0]);
            int longtitude_secs = (int)(0.006 * Integer.parseInt(temp[4].split("\\.")[1]));
            
			int latitude = Integer.parseInt(temp[2].split("\\.")[0]);
            int latitude_secs = (int)(0.006* Integer.parseInt(temp[2].split("\\.")[1]));
            
            String coordinates = String.valueOf(longtitude) + String.valueOf(longtitude_secs)
                                + String.valueOf(latitude) + String.valueOf(latitude_secs);
			T += "T=" + coordinates;
		}
        T += "\r";

		OutputStream gps = new FileOutputStream("../Pictures/M1.jpeg");

		int k;
		modem.write(T.getBytes());
		k = modem.read();

		while (k != -1){	

			gps.write(k);
			k = modem.read();

		}

		gps.flush();
		gps.close();

		System.out.println("gpsPackets END\n");
	}

	public void ARQPackets() throws IOException{

		System.out.println("Receiving ARQPackets...\n");

		BufferedWriter txt = new BufferedWriter(new FileWriter("../Data/text_ARQPackets.txt"));
		BufferedWriter log = new BufferedWriter(new FileWriter("../Data/log_ARQPackets.csv"));

        log.write("Packet,Response time,Times requested,Time elapsed");
        log.newLine();

		int reqs, count = 0;
		boolean isCorrect;
		long st = 0, start = System.currentTimeMillis();
		byte[] packet = new byte[58];

		while(System.currentTimeMillis()-start < duration){

			reqs = 0;
			isCorrect = false;

			st = System.currentTimeMillis();

			while(!isCorrect){

				modem.write(((reqs == 0)? ACK: NACK).getBytes());
				reqs++;

				//Save the packet: length of packet is 58
				for(int i=0; i < 58; i++){

					int k = modem.read();
					System.out.print((char)k);

					if (k == -1){

						System.out.println("Error -1");
						break;
					} 

					packet[i] = (byte)k;
				}

				isCorrect = check(packet);
			}
            count += 1;

			System.out.format("reqs = %d%n", reqs);

			txt.write(new String(packet));
			txt.newLine();

            log.write(String.valueOf(count) + ",");
			log.write(String.valueOf((System.currentTimeMillis()-st)) + ",");
            log.write(String.valueOf(reqs) + ",");
			log.write(String.valueOf((System.currentTimeMillis()-start)));
			log.newLine();		
		}

		txt.flush();
		txt.close();

		log.flush();
		log.close();

		System.out.println("ARQPackets END\n");
	}

	public boolean check(byte[] p){

		int xor = p[31];
		for(int i=32; i < 47; i++){
			xor = xor ^ p[i];
		}
		//ASCII to number conversion + calculation of 3-digit fcs
		int fcs = 100*(p[49]-48) + 10*(p[50]-48) + (p[51]-48);

		System.out.format(" xor: %d fcs: %d%n", xor, fcs);
		return (xor == fcs);
	}

	public void closeConnection(){

		System.out.println("Closing connection...");
		modem.close();
	}

	public static void main(String[] args) throws Exception{

		VirtualModem m = new VirtualModem();

		m.echoPackets();
		m.ARQPackets();
		m.imagePackets(IMAGE_REQEST_CODE, "E1");
		m.closeConnection();
		m = new VirtualModem();
		m.imagePackets(IMAGE_REQEST_CODE_ERRORS, "E2");
		m.closeConnection();
		m = new VirtualModem();
		m.gpsPackets();
	}
}