package com.github.niqdev.openwebnet.message;

import com.github.niqdev.openwebnet.OpenSession;
import rx.functions.Action0;
import rx.functions.Func1;

import java.util.List;

import static com.github.niqdev.openwebnet.message.Who.AUTOMATION;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * OpenWebNet Automation.
 *
 * <pre>
 * {@code
 *
 * import static com.github.niqdev.openwebnet.OpenWebNet.defaultGateway;
 *
 * // move shutter up
 * OpenWebNet
 *    .newClient(defaultGateway("IP_ADDRESS"))
 *    .send(Automation.requestMoveUp("WHERE"))
 *    .map(Automation.handleResponse(() -> System.out.println("success"), () -> System.out.println("fail")))
 *    .subscribe(System.out::println);
 * }
 * </pre>
 */
public class Automation extends BaseOpenMessage {

    public enum Type {
        // 0
        GENERAL,
        // [1−9]
        AREA,
        // #[1−9]
        GROUP,
        // [11-99]
        POINT
    }

    private static final int WHO = AUTOMATION.value();
    private static final int STOP = 0;
    private static final int UP = 1;
    private static final int DOWN = 2;

    private Automation(String value) {
        super(value);
    }

    /**
     * OpenWebNet message request to send the <i>STOP</i> automation command with value <b>*2*0*WHERE##</b>.
     *
     * @param where Value between 0 and 9999
     * @return message
     *
     * @deprecated use {@link Automation#requestStop(String, Type, String)}
     */
    public static Automation requestStop(String where) {
        checkRange(WHERE_MIN_VALUE, WHERE_MAX_VALUE, checkIsInteger(where));
        return new Automation(format(FORMAT_REQUEST, WHO, STOP, where));
    }

    /**
     * OpenWebNet message request to send the <i>STOP</i> automation command with value <b>*2*0*WHERE##</b>.
     *
     * @param where Value between 0 and 9999
     * @param type  Type {@link Type}
     * @param bus   Value
     * @return message
     */
    public static Automation requestStop(String where, Type type, String bus) {
        checkRangeType(where, type, bus);
        return new Automation(format(FORMAT_REQUEST, WHO, STOP, buildWhereValue(where, type, bus)));
    }

    /**
     * OpenWebNet message request to send the <i>UP</i> automation command with value <b>*2*1*WHERE##</b>.
     *
     * @param where Value between 0 and 9999
     * @return message
     *
     * @deprecated use {@link Automation#requestMoveUp(String, Type, String)}
     */
    public static Automation requestMoveUp(String where) {
        checkRange(WHERE_MIN_VALUE, WHERE_MAX_VALUE, checkIsInteger(where));
        return new Automation(format(FORMAT_REQUEST, WHO, UP, where));
    }

    /**
     * OpenWebNet message request to send the <i>UP</i> automation command with value <b>*2*1*WHERE##</b>.
     *
     * @param where Value between 0 and 9999
     * @param type  Type {@link Type}
     * @param bus   Value
     * @return message
     */
    public static Automation requestMoveUp(String where, Type type, String bus) {
        checkRangeType(where, type, bus);
        return new Automation(format(FORMAT_REQUEST, WHO, UP, buildWhereValue(where, type, bus)));
    }

    /**
     * OpenWebNet message request to send the <i>DOWN</i> automation command with value <b>*2*2*WHERE##</b>.
     *
     * @param where Value between 0 and 9999
     * @return message
     *
     * @deprecated use {@link Automation#requestMoveDown(String, Type, String)}
     */
    public static Automation requestMoveDown(String where) {
        checkRange(WHERE_MIN_VALUE, WHERE_MAX_VALUE, checkIsInteger(where));
        return new Automation(format(FORMAT_REQUEST, WHO, DOWN, where));
    }

    /**
     * OpenWebNet message request to send the <i>DOWN</i> automation command with value <b>*2*2*WHERE##</b>.
     *
     * @param where Value between 0 and 9999
     * @param type  Type {@link Type}
     * @param bus   Value
     * @return message
     */
    public static Automation requestMoveDown(String where, Type type, String bus) {
        checkRangeType(where, type, bus);
        return new Automation(format(FORMAT_REQUEST, WHO, DOWN, buildWhereValue(where, type, bus)));
    }

    /**
     * Handle response from {@link Automation#requestMoveUp(String)}, {@link Automation#requestMoveDown(String)} and {@link Automation#requestStop(String)}.
     *
     * @param onSuccess invoked if request has been successfully received
     * @param onFail    invoked otherwise
     * @return {@code Observable<OpenSession>}
     */
    public static Func1<OpenSession, OpenSession> handleResponse(Action0 onSuccess, Action0 onFail) {
        return handleResponse(onSuccess, onFail, WHO);
    }

    /**
     * OpenWebNet message request automation status with value <b>*#2*WHERE##</b>.
     *
     * @param where Value between 0 and 9999
     * @return message
     */
    public static Automation requestStatus(String where) {
        checkRange(WHERE_MIN_VALUE, WHERE_MAX_VALUE, checkIsInteger(where));
        return new Automation(format(FORMAT_STATUS, WHO, where));
    }

    /**
     * Handle response from {@link Automation#requestStatus(String)}.
     *
     * @param stopAction invoked if automation is stopped
     * @param upAction   invoked if automation is processing the <i>UP</i> command
     * @param downAction invoked if automation is processing the <i>DOWN</i> command
     * @return {@code Observable<OpenSession>}
     */
    public static Func1<OpenSession, OpenSession> handleStatus(Action0 stopAction, Action0 upAction, Action0 downAction) {
        return openSession -> {
            isValidPrefixType(openSession.getRequest(), FORMAT_PREFIX_STATUS_WHO, WHO);
            List<OpenMessage> response = openSession.getResponse();
            checkNotNull(response, "response is null");
            checkArgument(response.size() == 1 || response.size() == 2, "invalid response");
            final String responseValue = response.get(0).getValue();
            checkNotNull(responseValue, "response value is null");

            if (isStop(responseValue)) {
                stopAction.call();
                return openSession;
            }
            if (isUp(responseValue)) {
                upAction.call();
                return openSession;
            }
            if (isDown(responseValue)) {
                downAction.call();
                return openSession;
            }
            throw new IllegalStateException("unhandled automation status");
        };
    }

    /*
     * Verify OpenWebNet message response if automation is stopped.
     *
     * @param value
     * @return true if automation is processing the "STOP" command
     */
    static boolean isStop(String value) {
        return verifyMessage(value, STOP);
    }

    /*
     * Verify OpenWebNet message response if automation is up.
     *
     * @param value
     * @return true if automation is processing the "UP" command
     */
    static boolean isUp(String value) {
        return verifyMessage(value, UP);
    }

    /*
     * Verify OpenWebNet message response if automation is down.
     *
     * @param value
     * @return true if automation is processing the "DOWN" command
     */
    static boolean isDown(String value) {
        return verifyMessage(value, DOWN);
    }

    private static boolean verifyMessage(String value, int status) {
        return value != null && value.startsWith(format(FORMAT_PREFIX_RESPONSE, WHO, status))
            && value.length() > 7 && value.length() < 12 && value.endsWith(FRAME_END);
    }

    /*
     * GENERAL
     * 0
     *
     * AREA
     * [1−9]
     *
     * GROUP
     * #[1−9]
     *
     * POINT
     * [11-99]
     */
    private static void checkRangeType(String where, Type type, String bus) {
        checkNotNull(where, "invalid null value: [where]");
        checkNotNull(type, "invalid null value: [type]");
        checkNotNull(bus, "invalid null value: [bus]");
        checkArgument(where.length() >= 1 && where.length() <=2, "invalid length [1-2]");
        if (bus.length() != 0) {
            checkBus(bus);
        }
    }

    public static boolean isValidRangeType(String where, Type type, String bus) {
        throw new UnsupportedOperationException("not implemented");
    }

    private static String buildWhereValue(String where, Type type, String bus) {
        throw new UnsupportedOperationException("not implemented");
    }

}
