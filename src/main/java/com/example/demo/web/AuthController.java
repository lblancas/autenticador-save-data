package com.example.demo.web;
import static org.springframework.http.ResponseEntity.ok;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import com.example.demo.Tools;
import com.example.demo.bean.Response;
import com.example.demo.domain.BO;
import com.example.demo.domain.Session;
import com.example.demo.domain.User;
import com.example.demo.domain.catalogos.TipoUsuario;
import com.example.demo.domain.reglas.Cuenta;
import com.example.demo.domain.reglas.Persona;
import com.example.demo.repository.SesionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.catalogos.TipoUsuarioRepository;
import com.example.demo.repository.reglas.CuentaRepository;
import com.example.demo.repository.reglas.PersonaRepository;
import com.example.demo.security.jwt.JwtTokenProvider;
import com.example.demo.security.jwt.Token;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/auth")
@Api(value="API REST Servicio de autenticacion")
@CrossOrigin(origins="*")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
	CuentaRepository cuentaRepo;
    
    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    UserRepository users;
    
    @Autowired
    SesionRepository sessionRepository;
    
 
    @Autowired
	PersonaRepository  personaRepo;
    
    @Autowired
	TipoUsuarioRepository tipousuarioRepo; 
	
	@GetMapping("/obtieneInfo")
    public ResponseEntity<String> getInfo()
    {
    	return  new ResponseEntity((new String("Servicio activo")),HttpStatus.OK);
    }
    
    
    @PostMapping("/signin")
    @ApiOperation(value="Servicio que obtiene un token por medio de username / password. El campo cuenta es opcional {1,2,3}")
    public ResponseEntity signin(@RequestBody AuthenticationRequest data) 
    {
        try 
        {
        	String username = data.getUsername();
        	List<User> lista= users.findByUsernameAndActivo(username,1);
        	if(lista.size()<=0)
        		return  new ResponseEntity((new Response("Usuario invalido / o password incorrecto",400,(Object)"Problemas en logeo de usuario:"+data.getUsername())),HttpStatus.BAD_REQUEST);	
            Optional<User> usuarioOpcional= users.findByUsername(username);
        	User usuario  =usuarioOpcional.get();
        	if(usuario.getActivo()==0)// inactivo
        		return  new ResponseEntity((new Response("Usuario inactivo",400,(Object)"Problemas en logeo de usuario:"+username)),HttpStatus.BAD_REQUEST);
        	if(usuario.getIntentos()>= usuario.getMaximo_intentos())// inactivo
        		return  new ResponseEntity((new Response("Usuario bloqueado",400,(Object)"Problemas en logeo de usuario:"+username)),HttpStatus.BAD_REQUEST);
        	try
        	{
	        	if(usuario.getFecha_cambio_password().getTime()< (new Date()).getTime() )
	        		return  new ResponseEntity((new Response("Usuario con password expirada",400,(Object)"Problemas en logeo de usuario:"+username)),HttpStatus.BAD_REQUEST);
        	}catch(Exception dateEx)
        	{
        		return  new ResponseEntity((new Response("Usuario problema en fecha :Fecha_cambio_password",400,(Object)"Problemas en logeo de usuario:"+username)),HttpStatus.BAD_REQUEST);
        	}
        	if(existeSession(usuario))
        		return  new ResponseEntity((new Response("Usuario con sesion activa",400,(Object)"Problemas en logeo de usuario:"+username)),HttpStatus.BAD_REQUEST);
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, data.getPassword()));
            Persona persona = null;
            try
        	{
            	persona = (Persona)Tools.getBean(personaRepo.findByIdusuario(usuario));
        	}
        	catch(Exception ex)
            {
        		return  new ResponseEntity((new Response("No esta asignado a una persona el usuario ["+data.getUsername()+"]",400,(Object)"Problemas en logeo de usuario"+username)),HttpStatus.BAD_REQUEST);
            }
            
            
            TipoUsuario  tipousuario;
            try
            {
            	Long idTipoUsuario =  new Long(data.getCuenta());
            	try
            	{
            		tipousuario   =  (TipoUsuario)Tools.getBean(tipousuarioRepo.findById(idTipoUsuario));
            	}
            	catch(Exception ex)
                {
            		return  new ResponseEntity((new Response("Tipo de usuario invalido ["+data.getUsername()+"]",400,(Object)"Problemas en logeo de usuario:"+username)),HttpStatus.BAD_REQUEST);
                }
            }
            catch(Exception ex)
            {
            	System.out.println("No existe cuenta seleccionada : ["+data.getCuenta()+"]");
            }
            
            Map<Object, Object> cuentaModel = new HashMap<>();
            cuentaModel.put("tipousuario", persona.getIdtipousuario().getDescripcion());
            
            
            String tokenJWT  =  jwtTokenProvider.createToken(username, this.users.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Username " + username + "not found")).getRoles());
            Session sesionCreada= createSession(usuario,persona.getIdtipousuario().getId(),tokenJWT);
            
            Map<Object, Object> personaModel = new HashMap<>();
            cuentaModel.put("cuentas", getPerfiles(persona));
            personaModel.put("imagen", persona.getImagen());
            personaModel.put("banco", persona.getIdbanco().getDescripcion());
            personaModel.put("clabe", persona.getClabe());
            personaModel.put("tipousuarioDefault",persona.getIdtipousuario().getDescripcion());
            personaModel.put("cuenta", cuentaModel);
            
            
            Map<Object, Object> model = new HashMap<>();
            model.put("usuario", usuario.getUsername());
            model.put("id", usuario.getId());
            model.put("email", usuario.getEmail());
            model.put("nombre", usuario.getNombres());
            model.put("paterno", usuario.getPaterno());
            model.put("materno", usuario.getMaterno());
            model.put("rfc", persona.getRfc());
            model.put("telefono", usuario.getTelefono());
            model.put("persona", personaModel);
            model.put("caduca", sesionCreada.getFecha_inactividad());
            model.put("token",tokenJWT);
            return  new ResponseEntity((new Response("OK",200,(Object)model)),HttpStatus.OK);
        }
        catch (AuthenticationException e) 
        {
        	return  new ResponseEntity((new Response("Usuario invalido / o password incorrecto",400,(Object)"Problemas en logeo de usuario:"+data.getUsername())),HttpStatus.BAD_REQUEST);
        }
    }

	private boolean existeSession(User usuario) 
	{
		List<Session> listaSesssion = sessionRepository.findByUsuarioAndStatus((usuario.getId()).intValue(),1);
		if(listaSesssion.size()>0)
		{
			boolean modificoSessiones=false;
			for (Session sess: listaSesssion) 
			{
				long timeNow = (new Date()).getTime();
				long timeSess= sess.getFecha_inactividad().getTime();
				if(timeNow>timeSess)
				{
					sess.setStatus(0);
					sessionRepository.save(sess);
					modificoSessiones=true;
				}
				else
					return true;
			}
			if(modificoSessiones)
			{
				listaSesssion = sessionRepository.findByUsuarioAndStatus((usuario.getId()).intValue(),1);
				if(listaSesssion.size()>0)
					return true;
				return false;
			}
			
			return true;
		}
		return false;
	}
	private Session createSession(User usuario,Long idtipousuario,String sub) 
	{
		try
		{
			int seg =60;
    		int mil = 1000;
    		Date creacion = new Date(System.currentTimeMillis());
    		
    		Date now = new Date();
            now.setYear(now.getYear()+1);
            Date expiracion = new Date(now.getTime()); 
            
            Timestamp  creacionStamp = (new Timestamp(creacion.getTime()));  
            Timestamp  expiracionStamp = ( new Timestamp(expiracion.getTime()));
			Session se=new Session((usuario.getId()).intValue(),creacionStamp,expiracionStamp,1,idtipousuario,sub);
			Session sesionCreada=sessionRepository.save(se);
			return sesionCreada;
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	public List<BO> getPerfiles(Persona p)
    {
    	List<BO> bos=new ArrayList<BO>();
    	List<Cuenta> cuentas= cuentaRepo.findByIdpersona(p);
    	for (Cuenta c: cuentas)
    	{
    		BO bo=new BO(c.getIdtipousuario().getId(),c.getIdtipousuario().getDescripcion());
    		bos.add(bo);
    	}
    	return bos;
    }
}
