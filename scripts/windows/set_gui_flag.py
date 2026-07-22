#!/usr/bin/env python3

import sys


def set_gui_subsystem(path):
    with open(path, 'r+b') as f:
        f.seek(0x3C)
        pe_offset = int.from_bytes(f.read(4), 'little')

        subsystem_offset = pe_offset + 0x5C
        f.seek(subsystem_offset)

        f.write((2).to_bytes(2, 'little'))


if __name__ == "__main__":
    set_gui_subsystem(sys.argv[1])
