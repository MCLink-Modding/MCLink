/*
 * Copyright (c) 2017 - 2018 Dries007. All rights reserved
 */

package net.dries007.mclink;

import net.dries007.mclink.binding.FormatCode;
import net.dries007.mclink.binding.ISender;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
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

    private static TextFormatting getFormatCode(FormatCode formatCode)
    {
        switch (formatCode)
        {
            case BLACK:
                return TextFormatting.BLACK;
            case DARK_BLUE:
                return TextFormatting.DARK_BLUE;
            case DARK_GREEN:
                return TextFormatting.DARK_GREEN;
            case DARK_AQUA:
                return TextFormatting.DARK_AQUA;
            case DARK_RED:
                return TextFormatting.DARK_RED;
            case DARK_PURPLE:
                return TextFormatting.DARK_PURPLE;
            case GOLD:
                return TextFormatting.GOLD;
            case GRAY:
                return TextFormatting.GRAY;
            case DARK_GRAY:
                return TextFormatting.DARK_GRAY;
            case BLUE:
                return TextFormatting.BLUE;
            case GREEN:
                return TextFormatting.GREEN;
            case AQUA:
                return TextFormatting.AQUA;
            case RED:
                return TextFormatting.RED;
            case LIGHT_PURPLE:
                return TextFormatting.LIGHT_PURPLE;
            case YELLOW:
                return TextFormatting.YELLOW;
            case WHITE:
                return TextFormatting.WHITE;
            case OBFUSCATED:
                return TextFormatting.OBFUSCATED;
            case BOLD:
                return TextFormatting.BOLD;
            case STRIKETHROUGH:
                return TextFormatting.STRIKETHROUGH;
            case UNDERLINE:
                return TextFormatting.UNDERLINE;
            case ITALIC:
                return TextFormatting.ITALIC;
            case RESET:
                return TextFormatting.RESET;
        }
        throw new RuntimeException("Enum constant has magically disappeared?");
    }

    @NotNull
    @Override
    public String getName()
    {
        return sender.getName();
    }

    @Override
    public void sendMessage(String message)
    {
        sender.sendMessage(new TextComponentString(message));
    }

    @Override
    public void sendMessage(String message, FormatCode formatCode)
    {
        sender.sendMessage(new TextComponentString(message).setStyle(new Style().setColor(getFormatCode(formatCode))));
    }

    @Override
    public String toString()
    {
        return "SenderWrapper{" + sender.getName() + "}";
    }
}
