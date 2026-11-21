"""Serialize Maven writes to the shared reactor target directories across QA scripts."""
from contextlib import contextmanager
from pathlib import Path
import os
import time


@contextmanager
def backend_build_lock(root: Path, timeout: float = 600):
    path = root / '.runtime' / 'backend-build.lock'
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('a+b') as handle:
        deadline = time.monotonic() + timeout
        while True:
            try:
                handle.seek(0)
                if os.name == 'nt':
                    import msvcrt
                    msvcrt.locking(handle.fileno(), msvcrt.LK_NBLCK, 1)
                else:
                    import fcntl
                    fcntl.flock(handle.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
                break
            except (OSError, BlockingIOError):
                if time.monotonic() >= deadline:
                    raise TimeoutError('等待后端构建锁超时')
                time.sleep(.2)
        try:
            # Windows denies reading a byte locked by another process. Initialize
            # only after acquiring the lock, including an initially empty file.
            if os.fstat(handle.fileno()).st_size == 0:
                handle.write(b'0')
                handle.flush()
            yield
        finally:
            handle.seek(0)
            if os.name == 'nt':
                msvcrt.locking(handle.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                fcntl.flock(handle.fileno(), fcntl.LOCK_UN)
