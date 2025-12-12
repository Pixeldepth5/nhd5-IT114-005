package Client;

import Client.Interfaces.IClientEvents;
import Client.Interfaces.IConnectionEvents;
import Client.Interfaces.IMessageEvents;
import Client.Interfaces.IPhaseEvent;
import Client.Interfaces.IPointsEvent;
import Client.Interfaces.IQuestionEvent;
import Client.Interfaces.IReadyEvent;
import Client.Interfaces.IRoomEvents;
import Client.Interfaces.ITimeEvents;
import Client.Interfaces.ITurnEvent;
import Client.Interfaces.IUserListEvent;
import Common.Command;
import Common.ConnectionPayload;
import Common.Constants;
import Common.LoggerUtil;
import Common.Payload;
import Common.PayloadType;
import Common.Phase;
import Common.PointsPayload;
import Common.QAPayload;
import Common.ReadyPayload;
import Common.RoomAction;
import Common.RoomResultPayload;
import Common.TextFX;
import Common.TextFX.Color;
import Common.TimerPayload;
import Common.User;
import Common.UserListPayload;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;

/**
 * Demoing bi-directional communication between client and server in a
 * multi-client scenario
 */
public enum Client {
    INSTANCE;

    static {
        // statically initialize the client-side LoggerUtil
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024); // 2MB
        config.setFileCount(1);
        config.setLogLocation("client.log");
        // Set the logger configuration
        LoggerUtil.INSTANCE.setConfig(config);
    }
    private Socket server = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;
    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");
    private volatile boolean isRunning = true; // volatile for thread-safe visibility
    private final ConcurrentHashMap<Long, User> knownClients = new ConcurrentHashMap<>();
    private final User myUser = new User();
    private Phase currentPhase = Phase.READY;
    // callback that updates the UI
    private static final List<IClientEvents> events = new CopyOnWriteArrayList<>();
    private String currentRoom;
    private long hostClientId = Constants.DEFAULT_CLIENT_ID;

    private void error(String message) {
        LoggerUtil.INSTANCE.severe(TextFX.colorize(String.format("%s", message), Color.RED));
    }

    // needs to be private now that the enum logic is handling this
    private Client() {
        LoggerUtil.INSTANCE.info("Client Created");
    }

    public void registerCallback(IClientEvents e) {
        events.add(e);
    }

    /**
     * Used for client-side feedback
     * 
     * @param str
     */
    public void clientSideGameEvent(String str) {
        passToUICallback(IMessageEvents.class, e -> e.onMessageReceive(Constants.GAME_EVENT_CHANNEL, str));
    }

    public boolean isMyClientIdSet() {
        return myUser != null && myUser.getClientId() != Constants.DEFAULT_CLIENT_ID;
    }

    public boolean isMyClientId(long clientId) {
        return isMyClientIdSet() && myUser.getClientId() == clientId;
    }

    public boolean isHost() {
        return isMyClientId(hostClientId);
    }

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public long getHostClientId() {
        return hostClientId;
    }

    public boolean isConnected() {
        if (server == null) {
            return false;
        }
        // https://stackoverflow.com/a/10241044
        // Note: these check the client's end of the socket connect; therefore they
        // don't really help determine if the server had a problem
        // and is just for lesson's sake
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    /**
     * Takes an IP address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @param username
     * @return true if connection was successful
     */
    public boolean connect(String address, int port, String username) {
        myUser.setClientName(username);
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            LoggerUtil.INSTANCE.info("Client connected");
            // Use CompletableFuture to run listenToServer() in a separate thread
            CompletableFuture.runAsync(this::listenToServer);
            sendClientName(myUser.getClientName());// sync follow-up data (handshake)
        } catch (UnknownHostException e) {
            LoggerUtil.INSTANCE.severe(
                    String.format("Unable to connect to host %s:%d (unknown host)", address, port), e);
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe(
                    String.format("I/O error while connecting to %s:%d", address, port), e);
        }
        return isConnected();
    }

    /**
     * <p>
     * Check if the string contains the <i>connect</i> command
     * followed by an IP address and port or localhost and port.
     * </p>
     * <p>
     * Example format: 123.123.123.123:3000
     * </p>
     * <p>
     * Example format: localhost:3000
     * </p>
     * https://www.w3schools.com/java/java_regex.asp
     * 
     * @param text
     * @return true if the text is a valid connection command
     */
    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    /**
     * Controller for handling various text commands.
     * <p>
     * Add more here as needed
     * </p>
     * 
     * @param text
     * @return true if the text was a command or triggered a command
     * @throws IOException
     */
    private boolean processClientCommand(String text) throws IOException {
        boolean wasCommand = false;
        if (text.startsWith(Constants.COMMAND_TRIGGER)) {
            text = text.substring(1); // remove the /
            if (isConnection("/" + text)) {
                if (myUser.getClientName() == null || myUser.getClientName().isEmpty()) {
                    LoggerUtil.INSTANCE.warning(
                            TextFX.colorize("Please set your name via /name <name> before connecting", Color.RED));
                    return true;
                }
                String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
                connect(parts[0].trim(), Integer.parseInt(parts[1].trim()), myUser.getClientName());
                sendClientName(myUser.getClientName());// sync follow-up data (handshake)
                wasCommand = true;
            } else if (text.startsWith(Command.NAME.command)) {
                text = text.replace(Command.NAME.command, "").trim();
                if (text == null || text.length() == 0) {
                    LoggerUtil.INSTANCE
                            .warning(TextFX.colorize("This command requires a name as an argument", Color.RED));
                    return true;
                }
                myUser.setClientName(text);// temporary until we get a response from the server
                LoggerUtil.INSTANCE.info(TextFX.colorize(String.format("Name set to %s", myUser.getClientName()),
                        Color.YELLOW));
                wasCommand = true;
            } else if (text.equalsIgnoreCase(Command.LIST_USERS.command)) {
                String message = TextFX.colorize("Known clients:\n", Color.CYAN);
                LoggerUtil.INSTANCE.info(TextFX.colorize("Known clients:", Color.CYAN));
                message += String.join("\n", knownClients.values().stream()
                        .map(c -> String.format("%s %s %s %s",
                                c.getDisplayName(),
                                c.getClientId() == myUser.getClientId() ? " (you)" : "",
                                c.isReady() ? "[x]" : "[ ]",
                                c.didTakeTurn() ? "[T]" : "[ ]"))
                        .toList());
                LoggerUtil.INSTANCE.info(message);
                wasCommand = true;
            } else if (Command.QUIT.command.equalsIgnoreCase(text)) {
                close();
                wasCommand = true;
            } else if (Command.DISCONNECT.command.equalsIgnoreCase(text)) {
                sendDisconnect();
                wasCommand = true;
            } else if (text.startsWith(Command.REVERSE.command)) {
                text = text.replace(Command.REVERSE.command, "").trim();
                sendReverse(text);
                wasCommand = true;
            } else if (text.startsWith(Command.CREATE_ROOM.command)) {
                text = text.replace(Command.CREATE_ROOM.command, "").trim();
                if (text == null || text.length() == 0) {
                    LoggerUtil.INSTANCE
                            .warning(TextFX.colorize("This command requires a room name as an argument", Color.RED));
                    return true;
                }
                sendRoomAction(text, RoomAction.CREATE);
                wasCommand = true;
            } else if (text.startsWith(Command.JOIN_ROOM.command)) {
                text = text.replace(Command.JOIN_ROOM.command, "").trim();
                if (text == null || text.length() == 0) {
                    LoggerUtil.INSTANCE
                            .warning(TextFX.colorize("This command requires a room name as an argument", Color.RED));
                    return true;
                }
                sendRoomAction(text, RoomAction.JOIN);
                wasCommand = true;
            } else if (text.startsWith(Command.LEAVE_ROOM.command) || text.startsWith("leave")) {
                sendRoomAction(text, RoomAction.LEAVE);
                wasCommand = true;
            } else if (text.startsWith(Command.LIST_ROOMS.command)) {
                text = text.replace(Command.LIST_ROOMS.command, "").trim();

                sendRoomAction(text, RoomAction.LIST);
                wasCommand = true;
            }
        }
        return wasCommand;
    }

    // Start Send*() methods
    public void sendDoTurn(String text) throws IOException {
        ReadyPayload rp = new ReadyPayload();
        rp.setPayloadType(PayloadType.TURN);
        rp.setReady(true);
        rp.setMessage(text);
        sendToServer(rp);
    }

    public void sendReady() throws IOException {
        ReadyPayload rp = new ReadyPayload();
        rp.setPayloadType(PayloadType.READY);
        rp.setReady(true);
        sendToServer(rp);
    }

    public void sendRoomAction(String roomName, RoomAction roomAction) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(roomName);
        switch (roomAction) {
            case RoomAction.CREATE:
                payload.setPayloadType(PayloadType.ROOM_CREATE);
                break;
            case RoomAction.JOIN:
                payload.setPayloadType(PayloadType.ROOM_JOIN);
                break;
            case RoomAction.LEAVE:
                payload.setPayloadType(PayloadType.ROOM_LEAVE);
                break;
            case RoomAction.LIST:
                payload.setPayloadType(PayloadType.ROOM_LIST);
                break;
            default:
                LoggerUtil.INSTANCE.warning(TextFX.colorize("Invalid room action", Color.RED));
                break;
        }
        sendToServer(payload);
    }

    private void sendReverse(String message) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(message);
        payload.setPayloadType(PayloadType.REVERSE);
        sendToServer(payload);

    }

    public void sendDisconnect() throws IOException {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.DISCONNECT);
        sendToServer(payload);
    }

    public void sendMessage(String message) throws IOException {
        if (processClientCommand(message)) {
            return;
        }
        Payload payload = new Payload();
        payload.setMessage(message);
        payload.setPayloadType(PayloadType.MESSAGE);
        sendToServer(payload);
    }

    private void sendClientName(String name) throws IOException {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setClientName(name);
        payload.setPayloadType(PayloadType.CLIENT_CONNECT);
        sendToServer(payload);
    }

    private void sendToServer(Payload payload) throws IOException {
        if (isConnected()) {
            out.writeObject(payload);
            out.flush();
        } else {
            LoggerUtil.INSTANCE.warning(
                    "Not connected to server (hint: type `/connect host:port` without the quotes and replace host/port with the necessary info)");
        }
    }
    // End Send*() methods

    public void start() throws IOException {
        LoggerUtil.INSTANCE.info("Client starting");

        CompletableFuture<Void> inputFuture = CompletableFuture.runAsync(this::listenToInput);

        inputFuture.join();
    }

    private void listenToServer() {
        try {
            while (isRunning && isConnected()) {
                Payload fromServer = (Payload) in.readObject();
                if (fromServer != null) {
                    processPayload(fromServer);

                } else {
                    LoggerUtil.INSTANCE.info("Server disconnected");
                    break;
                }
            }
        } catch (ClassCastException | ClassNotFoundException cce) {
            LoggerUtil.INSTANCE.severe("Error reading object as specified type:", cce);
        } catch (IOException e) {
            if (isRunning) {
                LoggerUtil.INSTANCE.warning("Connection dropped", e);
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("Unexpected error in listenToServer()", e);
        } finally {
            closeServerConnection();
        }
        LoggerUtil.INSTANCE.info("listenToServer thread stopped");
    }

    private void processPayload(Payload payload) {
        switch (payload.getPayloadType()) {
            case CLIENT_CONNECT:// unused
                break;
            case CLIENT_ID:
                processClientData(payload);
                break;
            case DISCONNECT:
                processDisconnect(payload);
                break;
            case MESSAGE:
                processMessage(payload);
                break;
            case REVERSE:
                processReverse(payload);
                break;
            case ROOM_CREATE: // unused
                break;
            case ROOM_JOIN:
                processRoomAction(payload);
                break;
            case ROOM_LEAVE:
                processRoomAction(payload);
                break;
            case SYNC_CLIENT:
                processRoomAction(payload);
                break;
            case ROOM_LIST:
                processRoomsList(payload);
                break;
            case PayloadType.READY:
                processReadyStatus(payload, false);
                break;
            case PayloadType.SYNC_READY:
                processReadyStatus(payload, true);
                break;
            case PayloadType.RESET_READY:
                processResetReady();
                break;
            case PayloadType.PHASE:
                processPhase(payload);
                break;
            case PayloadType.TURN:
            case PayloadType.SYNC_TURN:
                processTurn(payload);
                break;
            case PayloadType.RESET_TURN:
                processResetTurn();
                break;
            case PayloadType.TIME:
                processCurrentTimer(payload);
                break;
            case PayloadType.POINTS:
            case PayloadType.POINTS_UPDATE:
                processPoints(payload);
                break;
            case PayloadType.QUESTION:
                processQuestion(payload);
                break;
            case PayloadType.USER_LIST:
                processUserList(payload);
                break;
            case PayloadType.TIMER:
                processCurrentTimer(payload);
                break;
            default:
                LoggerUtil.INSTANCE.warning(TextFX.colorize("Unhandled payload type", Color.YELLOW));
                break;

        }
    }

    public String getDisplayNameFromId(long id) {
        LoggerUtil.INSTANCE.info(String.format("Getting display name for client id %s", id));
        if (id == Constants.DEFAULT_CLIENT_ID) {
            return String.format("Room[%s]", currentRoom);
        }
        if (knownClients.containsKey(id)) {
            return knownClients.get(id).getDisplayName();
        }
        if (isMyClientId(id)) {
            return myUser.getDisplayName();
        }
        return "[Unknown]";
    }

    private <T> void passToUICallback(Class<T> type, java.util.function.Consumer<T> consumer) {
        Runnable work = () -> {
            try {
                for (IClientEvents event : events) {
                    if (type.isInstance(event)) {
                        consumer.accept(type.cast(event));
                    }
                }
            } catch (Exception e) {
                LoggerUtil.INSTANCE.severe("Error passing to callback", e);
            }
        };
        // Ensure Swing UI updates happen on the EDT to avoid input/menu deadlocks.
        if (SwingUtilities.isEventDispatchThread()) {
            work.run();
        } else {
            SwingUtilities.invokeLater(work);
        }
    }

    private void processPoints(Payload payload) {
        if (!(payload instanceof PointsPayload)) {
            error("Invalid payload subclass for processPoints");
            return;
        }
        PointsPayload pp = (PointsPayload) payload;
        long targetId = pp.getTargetClientId();
        int pts = pp.getPoints();
        if (targetId == Constants.DEFAULT_CLIENT_ID || targetId == 0) {
            knownClients.values().forEach(cp -> cp.setPoints(-1));

            passToUICallback(IPointsEvent.class, e -> e.onPointsUpdate(Constants.DEFAULT_CLIENT_ID, -1));
        } else if (knownClients.containsKey(targetId)) {
            knownClients.get(targetId).setPoints(pts);

            passToUICallback(IPointsEvent.class, e -> e.onPointsUpdate(targetId, pts));
        }
    }

    private void processCurrentTimer(Payload payload) {
        if (!(payload instanceof TimerPayload)) {
            error("Invalid payload subclass for processCurrentTimer");
            return;
        }
        TimerPayload timerPayload = (TimerPayload) payload;

        passToUICallback(ITimeEvents.class, e -> e.onTimerUpdate(timerPayload.getTimerType(), timerPayload.getTime()));
    }

    private void processResetTurn() {
        knownClients.values().forEach(cp -> cp.setTookTurn(false));
        passToUICallback(ITurnEvent.class, e -> e.onTookTurn(Constants.DEFAULT_CLIENT_ID, false));
    }

    private void processTurn(Payload payload) {
        if (!(payload instanceof ReadyPayload)) {
            error("Invalid payload subclass for processTurn");
            return;
        }
        ReadyPayload rp = (ReadyPayload) payload;
        if (!knownClients.containsKey(rp.getClientId())) {
            LoggerUtil.INSTANCE.severe(String.format("Received turn status for client id %s who is not known",
                    rp.getClientId()));
            return;
        }
        User cp = knownClients.get(rp.getClientId());
        cp.setTookTurn(rp.isReady());
        if (payload.getPayloadType() != PayloadType.SYNC_TURN) {
            String message = String.format("%s %s their turn", cp.getDisplayName(),
                    cp.didTakeTurn() ? "took" : "reset");
            LoggerUtil.INSTANCE.info(message);
            clientSideGameEvent(String.format("%s finished their turn",
                    cp.getDisplayName()));
        }

        passToUICallback(ITurnEvent.class, e -> e.onTookTurn(cp.getClientId(), cp.didTakeTurn()));

    }

    private void processPhase(Payload payload) {
        currentPhase = Enum.valueOf(Phase.class, payload.getMessage());
        passToUICallback(IPhaseEvent.class, e -> e.onReceivePhase(currentPhase));
    }

    private void processResetReady() {
        knownClients.values().forEach(cp -> {
            cp.setReady(false);
            cp.setTookTurn(false);
            cp.setPoints(-1);
        });

        passToUICallback(IReadyEvent.class, e -> e.onReceiveReady(Constants.DEFAULT_CLIENT_ID, false, true));
        passToUICallback(ITurnEvent.class, e -> e.onTookTurn(Constants.DEFAULT_CLIENT_ID, false));
        passToUICallback(IPointsEvent.class, e -> e.onPointsUpdate(Constants.DEFAULT_CLIENT_ID, -1));
    }

    private void processReadyStatus(Payload payload, boolean isQuiet) {
        if (!(payload instanceof ReadyPayload)) {
            error("Invalid payload subclass for processRoomsList");
            return;
        }
        ReadyPayload rp = (ReadyPayload) payload;
        if (!knownClients.containsKey(rp.getClientId())) {
            LoggerUtil.INSTANCE.severe(String.format("Received ready status [%s] for client id %s who is not known",
                    rp.isReady() ? "ready" : "not ready", rp.getClientId()));
            return;
        }
        User cp = knownClients.get(rp.getClientId());
        cp.setReady(rp.isReady());
        if (!isQuiet) {
            LoggerUtil.INSTANCE.info(
                    String.format("%s is %s", cp.getDisplayName(),
                            rp.isReady() ? "ready" : "not ready"));
        }

        passToUICallback(IReadyEvent.class, e -> e.onReceiveReady(cp.getClientId(), cp.isReady(), isQuiet));
    }

    private void processRoomsList(Payload payload) {
        if (!(payload instanceof RoomResultPayload)) {
            error("Invalid payload subclass for processRoomsList");
            return;
        }
        RoomResultPayload rrp = (RoomResultPayload) payload;
        List<String> rooms = rrp.getRooms();
        passToUICallback(IRoomEvents.class, e -> e.onReceiveRoomList(rooms, rrp.getMessage()));

        if (rooms == null || rooms.isEmpty()) {
            LoggerUtil.INSTANCE.warning(
                    TextFX.colorize("No rooms found matching your query",
                            Color.RED));
            return;
        }
        LoggerUtil.INSTANCE.info(TextFX.colorize("Room Results:", Color.PURPLE));
        LoggerUtil.INSTANCE.info(
                String.join(System.lineSeparator(), rooms));
    }

    private void processClientData(Payload payload) {
        if (myUser.getClientId() != Constants.DEFAULT_CLIENT_ID) {
            LoggerUtil.INSTANCE.warning(TextFX.colorize("Client ID already set, this shouldn't happen", Color.YELLOW));

        }
        myUser.setClientId(payload.getClientId());
        myUser.setClientName(((ConnectionPayload) payload).getClientName());
        knownClients.put(myUser.getClientId(), myUser);
        LoggerUtil.INSTANCE.info(TextFX.colorize("Connected", Color.GREEN));

        passToUICallback(IConnectionEvents.class, e -> e.onReceiveClientId(myUser.getClientId()));
    }

    private void processDisconnect(Payload payload) {
        passToUICallback(IConnectionEvents.class, e -> e.onClientDisconnect(payload.getClientId()));
        if (isMyClientId(payload.getClientId())) {
            knownClients.clear();
            myUser.reset();
            LoggerUtil.INSTANCE.info(TextFX.colorize("You disconnected", Color.RED));
        } else if (knownClients.containsKey(payload.getClientId())) {
            User disconnectedUser = knownClients.remove(payload.getClientId());
            if (disconnectedUser != null) {
                LoggerUtil.INSTANCE
                        .info(TextFX.colorize(String.format("%s disconnected", disconnectedUser.getDisplayName()),
                                Color.RED));
            }
        }

    }

    private void processRoomAction(Payload payload) {
        if (!(payload instanceof ConnectionPayload)) {
            error("Invalid payload subclass for processRoomAction");
            return;
        }
        ConnectionPayload connectionPayload = (ConnectionPayload) payload;
        if (connectionPayload.getClientId() == Constants.DEFAULT_CLIENT_ID) {
            knownClients.clear();

            passToUICallback(IRoomEvents.class, e -> e.onRoomAction(
                    Constants.DEFAULT_CLIENT_ID,
                    connectionPayload.getMessage(),
                    false,
                    true));
            return;
        }
        switch (connectionPayload.getPayloadType()) {

            case ROOM_LEAVE:
                if (knownClients.containsKey(connectionPayload.getClientId())) {
                    passToUICallback(IRoomEvents.class, e -> e.onRoomAction(
                            connectionPayload.getClientId(),
                            connectionPayload.getMessage(),
                            false,
                            false));
                    knownClients.remove(connectionPayload.getClientId());
                }
                if (connectionPayload.getMessage() != null) {
                    LoggerUtil.INSTANCE.info(TextFX.colorize(connectionPayload.getMessage(), Color.YELLOW));
                }

                break;
            case ROOM_JOIN:
                if (connectionPayload.getMessage() != null && isMyClientId(connectionPayload.getClientId())) {
                    currentRoom = connectionPayload.getMessage();
                    LoggerUtil.INSTANCE.info(TextFX.colorize(String.format("Joined %s", currentRoom), Color.GREEN));

                }
            case SYNC_CLIENT:
                if (!knownClients.containsKey(connectionPayload.getClientId())) {
                    User user = new User();
                    user.setClientId(connectionPayload.getClientId());
                    user.setClientName(connectionPayload.getClientName());
                    knownClients.put(connectionPayload.getClientId(), user);
                }
                passToUICallback(IRoomEvents.class, e -> e.onRoomAction(
                        connectionPayload.getClientId(),
                        connectionPayload.getMessage(),
                        true,
                        connectionPayload.getPayloadType() == PayloadType.SYNC_CLIENT));
                break;
            default:
                error("Invalid payload type for processRoomAction");
                break;
        }
    }

    private void processMessage(Payload payload) {
        LoggerUtil.INSTANCE.info(TextFX.colorize(payload.getMessage(), Color.BLUE));

        passToUICallback(IMessageEvents.class, e -> e.onMessageReceive(payload.getClientId(),
                payload.getMessage()));
    }

    private void processReverse(Payload payload) {
        LoggerUtil.INSTANCE.info(TextFX.colorize(payload.getMessage(), Color.PURPLE));

        passToUICallback(IMessageEvents.class, e -> e.onMessageReceive(payload.getClientId(),
                payload.getMessage()));
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    private void processQuestion(Payload payload) {
        if (!(payload instanceof QAPayload)) {
            error("Invalid payload subclass for processQuestion");
            return;
        }
        QAPayload qa = (QAPayload) payload;
        LoggerUtil.INSTANCE.info(TextFX.colorize("Question received: " + qa.getQuestionText(), Color.CYAN));
        passToUICallback(IQuestionEvent.class, e -> e.onQuestion(qa));
    }

    private void processUserList(Payload payload) {
        if (!(payload instanceof UserListPayload)) {
            error("Invalid payload subclass for processUserList");
            return;
        }
        UserListPayload ulp = (UserListPayload) payload;
        hostClientId = ulp.getHostClientId();
        // Populate knownClients from user list to avoid “unknown client” during ready sync
        knownClients.clear();
        for (int i = 0; i < ulp.getClientIds().size(); i++) {
            long id = ulp.getClientIds().get(i);
            String name = ulp.getDisplayNames().get(i);
            int pts = ulp.getPoints().get(i);
            boolean isReady = ulp.getLockedIn().get(i); // lockedIn used for UI; no ready flag here
            User u = new User();
            u.setClientId(id);
            u.setClientName(name);
            u.setPoints(pts);
            u.setReady(isReady); // best-effort; ready status will be sync’d separately
            knownClients.put(id, u);
        }
        LoggerUtil.INSTANCE
                .info(TextFX.colorize("User list updated: " + ulp.getClientIds().size() + " users", Color.CYAN));
        passToUICallback(IUserListEvent.class, e -> e.onUserListUpdate(ulp));
    }
    // End process*() methods

    @Deprecated
    private void listenToInput() {
        try (Scanner si = new Scanner(System.in)) {
            LoggerUtil.INSTANCE.info("Waiting for input");
            while (isRunning) {
                String userInput = si.nextLine();
                if (!processClientCommand(userInput)) {
                    sendMessage(userInput);
                }
            }
        } catch (IOException ioException) {
            LoggerUtil.INSTANCE.severe("Error in listenToInput()", ioException);
        }
        LoggerUtil.INSTANCE.info("listenToInput thread stopped");
    }

    private void close() {
        isRunning = false;
        closeServerConnection();
        LoggerUtil.INSTANCE.info("Client terminated");
    }

    private void closeServerConnection() {
        try {
            if (out != null) {
                LoggerUtil.INSTANCE.info("Closing output stream");
                out.close();
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.warning("Failed to close output stream", e);
        }
        try {
            if (in != null) {
                LoggerUtil.INSTANCE.info("Closing input stream");
                in.close();
            }
        } catch (Exception e) {
            LoggerUtil.INSTANCE.warning("Failed to close input stream", e);
        }
        try {
            if (server != null) {
                LoggerUtil.INSTANCE.info("Closing connection");
                server.close();
                LoggerUtil.INSTANCE.info("Closed Socket");
            }
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Socket error while closing connection", e);
        }
    }

    @Deprecated
    public static void main(String[] args) {
        Client client = Client.INSTANCE;
        try {
            client.start();
        } catch (IOException e) {
            LoggerUtil.INSTANCE.severe("Exception from main()", e);
        }
    }
}
