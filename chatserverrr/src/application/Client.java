package application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

// chatserverrr가 한명의 클라이언트와 통신을 하기 위해서 필요한 기능들을 아래 Client 클래스에 정의하겠다는 뜻.
// 즉 한명의 클라이언트와 통신하도록 해주는 client class라고 할 수 있음
public class Client {
	Socket socket; //하나의 소켓 만들기, 소켓이 있어야 어떠한 컴퓨터와 네트워크상에서 통신할 수 있음
	/* 생성자를 만들어 준다. 즉 어떠한 변수의 초기화를 위해서 만드는 것임. */
	public Client(Socket socket) { //매개변수로 넘어오는 것을 초기화 시키는 것
		this.socket = socket;
		receive();
	}
	
	//클라이언트로부터 메시지를 전달 받는 메소드
	public void receive() {
		Runnable thread = new Runnable(){ //일반적으로 하나의 스레드를 만들때 Runnable을 많이 사용함

			@Override
			public void run() {// 하나의 스레드가 어떠한 모듈로써 동작을 할건지 run()안에서 정의.
				try {  
					while(true) {// 반복적으로 클라이언트로부터 내용을 전달받기 위함.
						InputStream in = socket.getInputStream(); //어떠한 내용을 전달받을 수 있도록 inputstream객체를 사용
						byte[] buffer = new byte[512]; // 버퍼를 이용해서 한번에 512byte만큼 전달을 받을 수 있도록
						int length = in.read(buffer); //클라이언트로부터 받은 내용을 버퍼로 저장
						while(length == -1) throw new IOException(); //담긴 메시지의 크기가 -1이라면 즉, 오류라면 오류라는 메시지를 띄어줌
						System.out.println("[메시지 수신 성공] "
								+ socket.getRemoteSocketAddress() //현재 접속한 클라이언트 주소 정보 출력
								+ ": "+Thread.currentThread()); //스레드의 고유 정보 출력
						String message = new String(buffer, 0, length ,"UTF-8"); // 전달받은 내용을 message라는 변수에 담아서 출력
						for(Client client : Main.clients) { // 전달 받은 메시지를 다른 클라이언트들도 받을 수 있게
							client.send(message); //client send함수 불러옴
						}
					}
				}catch(Exception e) { // 예외 발생
					try {
						System.out.println("[메시지 수신 오류] "
								+socket.getReuseAddress()
								+": "+Thread.currentThread().getName());
					}catch(Exception e2) {
						e2.printStackTrace();
					}
				}
				
			}
			
		};
		Main.threadPool.submit(thread);// threadPool에 만들어진 스레드를 등록하겠다. 스레드를 안정적으로 관리하기 위함.
	}
	
	//클라이언트에게 메시지를 전송하는 메소드
	public void send(String message) {
		Runnable thread = new Runnable() {

			@Override
			public void run() {
				try {
				OutputStream out = socket.getOutputStream();
				byte[] buffer = message.getBytes("UTF-8"); //메시지를 보낼 땐 outputstream을 이용해서 전송
				out.write(buffer); //버퍼에 담긴 내용을 서버에서 클라이언트로 전송
				out.flush(); //전송완료임을 알려줌.
				} catch(Exception e) {
					try {
						System.out.println("[메시지 송신 오류]"
								+socket.getRemoteSocketAddress()
								+": "+Thread.currentThread().getName());
						Main.clients.remove(Client.this); //모든 클라이언트 정보를 담는 일종의 배열에서 현재 존재하는 클라이언트를 지워줌
						socket.close(); //오류가 생긴 클라이언트의 소켓 닫음
					} catch (Exception e2) {
						
					}
				}
				
			}
			
		};
		Main.threadPool.submit(thread);
	}
}
