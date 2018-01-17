/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.ISender;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dries007
 */
@SuppressWarnings("Duplicates")
public class SenderWrapper implements ISender
{
    private final ICommandSender sender;

    SenderWrapper(ICommandSender sender)
    {
        this.sender = sender;
    }

    private static EnumChatFormatting getFormatCode(FormatCode formatCode)
    {
        switch (formatCode)
        {
            case BLACK:
                return EnumChatFormatting.BLACK;
            case DARK_BLUE:
                return EnumChatFormatting.DARK_BLUE;
            case DARK_GREEN:
                return EnumChatFormatting.DARK_GREEN;
            case DARK_AQUA:
                return EnumChatFormatting.DARK_AQUA;
            case DARK_RED:
                return EnumChatFormatting.DARK_RED;
            case DARK_PURPLE:
                return EnumChatFormatting.DARK_PURPLE;
            case GOLD:
                return EnumChatFormatting.GOLD;
            case GRAY:
                return EnumChatFormatting.GRAY;
            case DARK_GRAY:
                return EnumChatFormatting.DARK_GRAY;
            case BLUE:
                return EnumChatFormatting.BLUE;
            case GREEN:
                return EnumChatFormatting.GREEN;
            case AQUA:
                return EnumChatFormatting.AQUA;
            case RED:
                return EnumChatFormatting.RED;
            case LIGHT_PURPLE:
                return EnumChatFormatting.LIGHT_PURPLE;
            case YELLOW:
                return EnumChatFormatting.YELLOW;
            case WHITE:
                return EnumChatFormatting.WHITE;
            case OBFUSCATED:
                return EnumChatFormatting.OBFUSCATED;
            case BOLD:
                return EnumChatFormatting.BOLD;
            case STRIKETHROUGH:
                return EnumChatFormatting.STRIKETHROUGH;
            case UNDERLINE:
                return EnumChatFormatting.UNDERLINE;
            case ITALIC:
                return EnumChatFormatting.ITALIC;
            case RESET:
                return EnumChatFormatting.RESET;
        }
        throw new RuntimeException("Enum constant has magically disappeared?");
    }

    @NotNull
    @Override
    public String getName()
    {
        return sender.getCommandSenderName();
    }

    @Override
    public void sendMessage(String message)
    {
        sender.addChatMessage(new ChatComponentText(message));
    }

    @Override
    public void sendMessage(String message, FormatCode formatCode)
    {
        sender.addChatMessage(new ChatComponentText(message).setChatStyle(new ChatStyle().setColor(getFormatCode(formatCode))));
    }

    @Override
    public String toString()
    {
        return "SenderWrapper{" + sender.getCommandSenderName() + "}";
    }
}
