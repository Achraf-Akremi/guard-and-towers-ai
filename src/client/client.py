"""
Game-server client for the Guard & Towers AI.

Connects to the course game server, waits for its turn, asks the Java AI
(game.jar) for the best move and sends it back.

Usage:
    python client.py                          # auto-detects game.jar
    python client.py --jar path/to/game.jar   # explicit path
    GAME_JAR=path/to/game.jar python client.py

Requires the course-provided network.py in the same folder.
"""

import argparse
import json
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

from network import Network

SCRIPT_DIR = Path(__file__).resolve().parent

# Places to look for the JAR when no path is given (first match wins).
JAR_CANDIDATES = [
    SCRIPT_DIR / "game.jar",
    SCRIPT_DIR / "Game.jar",
    SCRIPT_DIR.parent / "game.jar",
    SCRIPT_DIR.parent / "Game.jar",
    SCRIPT_DIR.parent / "src" / "game.jar",
    SCRIPT_DIR.parent / "src" / "Game.jar",
]


def parse_args():
    parser = argparse.ArgumentParser(description="Guard & Towers game-server client")
    parser.add_argument("--jar", default=os.environ.get("GAME_JAR"),
                        help="Path to game.jar (default: $GAME_JAR or auto-detect)")
    parser.add_argument("--java", default=os.environ.get("JAVA_BIN", "java"),
                        help="Java executable (default: $JAVA_BIN or 'java')")
    parser.add_argument("--timeout", type=float, default=5.0,
                        help="Seconds to wait for the AI before giving up (default: 5)")
    parser.add_argument("--poll", type=float, default=1 / 60,
                        help="Seconds between server polls (default: 1/60)")
    parser.add_argument("--fallback-move", default=None,
                        help="Move to send if the AI fails (default: none, skip)")
    return parser.parse_args()


def find_jar(explicit_path):
    """Return the path to game.jar, or exit with a helpful message."""
    if explicit_path:
        path = Path(explicit_path).expanduser().resolve()
        if path.is_file():
            return path
        sys.exit(f"JAR not found: {path}")

    for candidate in JAR_CANDIDATES:
        if candidate.is_file():
            return candidate

    searched = "\n  ".join(str(c) for c in JAR_CANDIDATES)
    sys.exit(f"Could not find game.jar. Searched:\n  {searched}\n"
             "Pass it with --jar or set the GAME_JAR environment variable.")


def check_java(java_bin):
    if shutil.which(java_bin) is None:
        sys.exit(f"Java executable '{java_bin}' not found. "
                 "Install Java 24+ or pass --java /path/to/java.")


def build_position(board, turn):
    """The Java AI expects 'BOARD TURN'. Add the turn if the server omitted it."""
    board = board.strip()
    return board if " " in board else f"{board} {turn}"


def compute_move(java_bin, jar_path, position, timeout):
    """Run the Java AI on a position and return its move, e.g. 'A7-B7-1'."""
    try:
        proc = subprocess.run(
            [java_bin, "-jar", str(jar_path), position],
            capture_output=True,
            text=True,
            timeout=timeout,
        )
    except subprocess.TimeoutExpired:
        raise RuntimeError(f"Java AI timed out after {timeout}s")

    if proc.returncode != 0:
        raise RuntimeError(f"Java AI failed:\n{proc.stderr.strip()}")

    # Take the last non-empty line, in case the AI prints debug output first.
    lines = [line.strip() for line in proc.stdout.splitlines() if line.strip()]
    if not lines:
        raise RuntimeError("Java AI returned no move")
    return lines[-1]


def main():
    args = parse_args()
    check_java(args.java)
    jar_path = find_jar(args.jar)
    print(f"Using AI: {jar_path}")

    net = Network()
    player = int(net.getP())
    my_color = "r" if player == 0 else "b"
    print(f"You are player {player} ({'red' if my_color == 'r' else 'blue'})")

    last_answered_board = None

    try:
        while True:
            time.sleep(args.poll)

            try:
                raw = net.send(json.dumps("get"))
                if raw is None:
                    raise ValueError("no game data received")
                game = json.loads(raw)
            except Exception as e:
                print("Network error:", e)
                break

            if not game.get("bothConnected"):
                continue
            if game.get("turn") != my_color:
                continue
            # Don't answer the same position twice while the server updates.
            if game["board"] == last_answered_board:
                continue

            print(f"Board: {game['board']}  Time left: {game.get('time')}")
            position = build_position(game["board"], my_color)

            try:
                move = compute_move(args.java, jar_path, position, args.timeout)
            except RuntimeError as e:
                print("AI error:", e)
                if args.fallback_move is None:
                    continue
                move = args.fallback_move
                print("Using fallback move")

            print("→ sending move:", move)
            net.send(json.dumps(move))
            last_answered_board = game["board"]

    except KeyboardInterrupt:
        print("\nStopped by user.")


if __name__ == "__main__":
    main()