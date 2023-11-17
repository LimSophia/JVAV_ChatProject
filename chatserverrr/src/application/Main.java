package application;
	
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;


public class Main extends Application {
	/*스레드 풀이라는 것을 이용해서 다양한 클라이언트가 접속했을 때 스레드들을 효과적으로 관리할 수 있도록 함.
	 * Vector 라이브러리를 이용해서 접속한 클라이언트들을 관리*/
	/*, threadPool로 스레드를 처리하게 되면 기본적인 스레드 숫자에 제한을 두기 대문에 갑작스럽게 클라이언트 숫자가 폭증하더라도 스레드의 숫자에는 제한이 있기 때문에 서버의 성능저하를 방지할 수 있음*/
	public static ExecutorService threadPool; // ExecutorService는 여러 개의 스레드를 효율적으로 관리하는 위해 사용하는 대표적인 라이브러리
	public static Vector<Client> clients = new Vector<Client>();
	
	ServerSocket serverSocket;
	
	// 서버를 구동시켜서 클라이언트의 연결을 기다리는 메소드
	public void startServer(String IP, int port) {//클라이언트와 통신을 할것인지
		try {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(IP, port)); // 바인드로 서버 컴퓨터 역할을 수행하는 컴퓨터가 자신의 IP,port번호로 특정 클라이언트 접속을 기다림
		} catch(Exception e) {
			e.printStackTrace();
			if(!serverSocket.isClosed()) { //서버가 닫혀있는 상태가 아니라면
				stopServer(); //서버를 종료
			}
			return;
		}
		
		//클라이언트가 접속할 때까지 계속 기다리는 스레드
		Runnable thread = new Runnable() {

			@Override
			public void run() {
				while(true) {
					try {
						Socket socket = serverSocket.accept(); //새로운 클라이언트가 접속할 수 있도록
						clients.add(new Client(socket)); // 클라이언트 추가
						System.out.println("[클라이언트 접속] "
								+socket.getRemoteSocketAddress()
								+": "+Thread.currentThread().getName());
			 		}catch(Exception e) {
			 			if(!serverSocket.isClosed()) {
			 				stopServer();
			 			}
			 			break;
			 		}
				}
			}
			
		};
		threadPool = Executors.newCachedThreadPool(); //스레드풀 초기화
		threadPool.submit(thread); //threadPool에 현재 클라이언트를 기다리는 스레드를 담고 첫 번재 스레드를 넣어줌
	}
	
	// 서버의 작동을 중지시키는 메소드 -> 서버 작동종료 이후에 전체 자원을 할당해제해주는 함수
	public void stopServer() {
		try {
			//현재 작동 중인 모든 소켓 닫기
			Iterator<Client> iterator = clients.iterator(); //모든 클라이언트에 개별적으로 접근할 수 있도록
			while(iterator.hasNext()) { //하나씩 접근할 수 있도록 -> hasNext()
				Client client = iterator.next(); // 특정 클라이언트에 접근해서 
				client.socket.close(); // 그 클라이언트 닫기
				iterator.remove(); //해당 연결이 끊긴 클라이언트 지우기
			}
			//서버소켓 객체 닫기
			if(serverSocket != null && !serverSocket.isClosed()) { //서버 소켓이 null값이 아니고 열어 있다면 닫기
				serverSocket.close();
			}
			//스레드 풀 종료
			if(threadPool != null && !threadPool.isShutdown()) { //스레드 풀 또한 종료(안에 어떠한 스레드도 남아있지 않기 때문에)
				threadPool.shutdown();
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	// UI를 생성하고, 실질적으로 프로그램을 동작시키는 메소드
	@Override
	public void start(Stage primaryStage) {
		BorderPane root = new BorderPane(); //전체의 디자인 툴을 담을 수 잇는 하나의 pane
		root.setPadding(new Insets(5));
		
		TextArea textArea = new TextArea(); //긴 문장의 텍스트가 담길 수 있는 공간 TextArea
		textArea.setEditable(false); //단순히 출력만 할 수 있도록하고 수정이 불가하도록
		textArea.setFont(new Font("나눔고딕", 15)); //폰트는 컴퓨터에 설치되어있는 폰트로 
		root.setCenter(textArea);
		
		Button toggleButton = new Button("시작하기"); //서버의 작동을 시작하도록 해주는 버튼
		toggleButton.setMaxWidth(Double.MAX_VALUE); //toggle은 스위치라고 생각하기, 즉 시작을 한 다음에 종료할 수 있도록
		BorderPane.setMargin(toggleButton, new Insets(1,0,0,0)); //BorderPane에 Margin을 줘서 디자인적으로 
		root.setBottom(toggleButton); //버튼을 담을 수 있도록 
		
		String IP = "127.0.0.1"; //이 주소는 자기 자신의 컴퓨터 주소; 내 컴 안에서 테스트를 하겠다는 뜻
		int port = 9876;
		
		toggleButton.setOnAction(event ->{ //버튼에 대한 액션 처리
			if(toggleButton.getText().equals("시작하기")) { //버튼이 시작하기 문자열을 포함한 상태라면
				startServer(IP, port); //서버를 시작
				Platform.runLater(()->{ //플랫폼이라고 해서 runLater함수를 이용해서 GUI요소를 출력
					String message = String.format("[서버 시작]\n", IP, port); //서버시작 출력
					textArea.appendText(message); //textArea에다가 메시지를 출력하도록 만들 수 있음
					toggleButton.setText("종료하기"); //버튼 내용을 "종료하기"로 바꿔주기
				});
			}else {
				stopServer(); //종료 버튼 누르면 서버 종료
				Platform.runLater(()->{ //화면에 어떤 내용을 출력할 수 있도록 플랫폼 runLater
					String message = String.format("[서버 종료]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("시작하기"); //다시 시작할 수 있도록 버튼의 내용을 변경
				});
			}
		});
		Scene scene = new Scene(root, 400, 400); //화면 크기 구성(400*400)
		primaryStage.setTitle("[채팅 서버]"); //제목 설정
		primaryStage.setOnCloseRequest(event -> stopServer()); //프로그램 자체를 종료했다면 stopServer함수 수행 후 종료
		primaryStage.setScene(scene); //씬 정보를 설정(위에서 만들었던 씬 정보를 화면에 정상 출력할 수 있도록)
		primaryStage.show();
		}
	
	// 프로그램의 진입
	public static void main(String[] args) {
		launch(args);
	}
}
