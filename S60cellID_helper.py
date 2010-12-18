# adapted from the application skeleton in the sdk

import e32
import appuifw
import location
import struct
import socket
import threading

class gsm_location :
	def __init__(self) :
		self.text = u""
		self.noRefresh = 0;

	def gsm_location(self) :
		self.noRefresh = self.noRefresh + 1;
		try:
			(self.mcc, self.mnc, self.lac, self.cellid) = location.gsm_location()
		except:
			self.mcc = 0
			self.mnc = 0
			self.lac = 0
			self.cellid = 0
		self.text = u"MCC: %s\nMNC: %s\nLAC: %s\nCell id: %s\nnoSamples: %i\n" % (self.mcc, self.mnc, self.lac, self.cellid, self.noRefresh)
		return self.text

	def close(self) :
		pass

e32.ao_yield()

class Client(threading.Thread):
	def __init__(self, app):
		threading.Thread.__init__(self)
		try:
			print('Starting up Socket ')
			self.serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
			self.serversocket.bind(("localhost", 59721))
			self.serversocket.setsockopt(socket.SOL_TCP, socket.TCP_NODELAY, 1)
			self.serversocket.listen(1)
		except socket.error, msg:
			self.serversocket = None
			print('Failed ' + str(msg));
		self.app = app
		self.exit_flag = 0

	def run(self):
		while not self.exit_flag:
			print('Waiting for socket connection')
			(clientsocket, address) = self.serversocket.accept()
			print('Got connection!')
			try:
				while 1:
					cmdS = ''
					while len(cmdS) < 4:
						chunk = clientsocket.recv(4);
						cmdS = cmdS + chunk
						print('Cmd receivedd so far: ' + str(len(cmdS)) + ' ' + cmdS)
						if len(chunk) == 0:
							raise socket.error, "socket connection broken"
					cmd = struct.unpack('>i',cmdS)
					if (cmd[0] == 6574723):
						signal = 0;
						msg = struct.pack('>iiiih', self.app.db.mcc, self.app.db.mnc, self.app.db.lac, self.app.db.cellid, signal) + '\n'
						totalsent = 0
						msglen = len(msg)
						while totalsent < msglen:
							sent = clientsocket.send(msg[totalsent:])
							if sent == 0:
								raise socket.error, "socket connection broken"
							totalsent = totalsent + sent
			except socket.error, msg:
				clientsocket = None
				self.exit_flag = 0


class gsm_location_app:
    def __init__(self):
        self.lock = e32.Ao_lock()

        self.old_title = appuifw.app.title
        appuifw.app.title = u"GSM Location"

        self.exit_flag = False
        appuifw.app.exit_key_handler = self.abort

        self.db = gsm_location()
		
        appuifw.app.body = appuifw.Text()
        appuifw.app.menu = [(u"Refresh", self.refresh)] 

    def loop(self):
        try:
            self.refresh()
            e32.ao_sleep(1)
            while not self.exit_flag:
                self.refresh()
                e32.ao_sleep(1)
        finally:
            self.db.close()

    def close(self):
        appuifw.app.menu = []
        appuifw.app.body = None
        appuifw.app.exit_key_handler = None
        appuifw.app.title = self.old_title

    def abort(self):
        # Exit-key handler.
        self.exit_flag = True
        self.lock.signal()

    def refresh(self):
		self.db.gsm_location()
		appuifw.app.body.set(self.db.text)

def main():
    app = gsm_location_app()
    c = Client(app)
    c.start()
    try:
        app.loop()
    finally:
        app.close()

if __name__ == "__main__":
    main()
 

