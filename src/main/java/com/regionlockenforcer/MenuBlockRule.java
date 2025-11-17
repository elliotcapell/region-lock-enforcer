package com.regionlockenforcer;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class MenuBlockRule
{
    private boolean enabled = true;
    private List<Integer> opcodes = new ArrayList<>(); // MenuAction ids (optional)
    private String optionRegex;                        // e.g. (?i).*teleport.*|.*travel.*|.*charter.*|.*quetzal.*
    private String targetRegex;                        // e.g. (?i).*Varrock.*|.*Ardougne.*

    boolean matches(int type, String option, String target)
    {
        if (!opcodes.isEmpty() && !opcodes.contains(type)) return false;
        if (optionRegex != null && option != null)
        {
            try { if (!option.matches(optionRegex)) return false; } catch (Exception ignored) { return false; }
        }
        if (targetRegex != null && target != null)
        {
            try { if (!target.matches(targetRegex)) return false; } catch (Exception ignored) { return false; }
        }
        return true;
    }
}
