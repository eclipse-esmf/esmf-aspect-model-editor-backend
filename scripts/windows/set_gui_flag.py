#!/usr/bin/env python3

import pefile
import shutil
import sys

from tempfile import NamedTemporaryFile


def set_subsystem_type(path, subsystem_type):
    with open(path, 'rb') as f:
        original_data = f.read()

    with pefile.PE(path, fast_load=True) as pe:
        pe_end = max(s.PointerToRawData + s.SizeOfRawData for s in pe.sections)
        pe.OPTIONAL_HEADER.Subsystem = dict(pefile.subsystem_types)[subsystem_type]
        temp = NamedTemporaryFile(delete=False)
        pe.write(temp.name)
        temp.close()

    # Append overlay data (warp tarball)
    overlay = original_data[pe_end:]
    if overlay:
        with open(temp.name, 'ab') as f:
            f.write(overlay)

    shutil.move(temp.name, path)


if __name__ == "__main__":
    set_subsystem_type(sys.argv[1], "IMAGE_SUBSYSTEM_WINDOWS_GUI")
