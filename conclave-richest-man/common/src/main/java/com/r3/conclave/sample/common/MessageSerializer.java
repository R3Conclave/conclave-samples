package com.r3.conclave.sample.common;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class MessageSerializer extends Serializer<Message> {

    @Override
    public void write(Kryo kryo, Output output, Message object) {
        output.writeString(object.getType());
        output.writeInt(object.getNetworth());
    }

    @Override
    public Message read(Kryo kryo, Input input, Class<Message> type) {
        String cmd = input.readString();
        Integer bid = input.readInt();
        Message message = new Message(cmd, bid);
        return message;
    }
}
